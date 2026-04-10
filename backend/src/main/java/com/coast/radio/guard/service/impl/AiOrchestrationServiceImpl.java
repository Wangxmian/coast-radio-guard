package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.ai.client.AsrClient;
import com.coast.radio.guard.ai.client.LlmClient;
import com.coast.radio.guard.ai.client.SeClient;
import com.coast.radio.guard.common.constants.*;
import com.coast.radio.guard.dto.ai.AsrTranscribeResponseDTO;
import com.coast.radio.guard.dto.ai.LlmAnalyzeResponseDTO;
import com.coast.radio.guard.dto.ai.LlmEntityDTO;
import com.coast.radio.guard.dto.ai.SeEnhanceResponseDTO;
import com.coast.radio.guard.entity.*;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.*;
import com.coast.radio.guard.service.AiOrchestrationService;
import com.coast.radio.guard.service.AlarmService;
import com.coast.radio.guard.service.TranscriptCorrectionService;
import com.coast.radio.guard.service.model.TranscriptCorrectionResult;
import com.coast.radio.guard.vo.audio.*;
import com.coast.radio.guard.vo.monitor.RealtimeChunkResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AiOrchestrationServiceImpl implements AiOrchestrationService {

    private static final String TASK_TYPE_REALTIME = "REALTIME";
    private static final String TASK_TYPE_OFFLINE = "OFFLINE";
    private static final List<String> ALERT_KEYWORDS = List.of(
        "mayday", "fire", "collision", "sinking", "help", "man overboard", "immediate assistance"
    );

    private final AudioTaskMapper audioTaskMapper;
    private final AsrResultMapper asrResultMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;
    private final EntityResultMapper entityResultMapper;
    private final RiskEventMapper riskEventMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final SeClient seClient;
    private final AsrClient asrClient;
    private final LlmClient llmClient;
    private final AlarmService alarmService;
    private final TranscriptCorrectionService transcriptCorrectionService;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public AiOrchestrationServiceImpl(AudioTaskMapper audioTaskMapper,
                                      AsrResultMapper asrResultMapper,
                                      LlmAnalysisResultMapper llmAnalysisResultMapper,
                                      EntityResultMapper entityResultMapper,
                                      RiskEventMapper riskEventMapper,
                                      AlarmRecordMapper alarmRecordMapper,
                                      SeClient seClient,
                                      AsrClient asrClient,
                                      LlmClient llmClient,
                                      AlarmService alarmService,
                                      TranscriptCorrectionService transcriptCorrectionService,
                                      SystemConfigMapper systemConfigMapper,
                                      ObjectMapper objectMapper) {
        this.audioTaskMapper = audioTaskMapper;
        this.asrResultMapper = asrResultMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
        this.entityResultMapper = entityResultMapper;
        this.riskEventMapper = riskEventMapper;
        this.alarmRecordMapper = alarmRecordMapper;
        this.seClient = seClient;
        this.asrClient = asrClient;
        this.llmClient = llmClient;
        this.alarmService = alarmService;
        this.transcriptCorrectionService = transcriptCorrectionService;
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExecuteSummaryVO executeTask(Long taskId) {
        AudioTask task = requireTask(taskId);
        if (TASK_TYPE_REALTIME.equalsIgnoreCase(task.getTaskType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "实时监听任务不支持离线执行，请在值守监控中心继续处理或新建离线音频任务");
        }
        LocalDateTime now = LocalDateTime.now();

        task.setTaskStatus(TaskStatus.PROCESSING);
        task.setSeStatus(TaskStatus.PROCESSING);
        task.setAsrStatus(TaskStatus.WAITING);
        task.setLlmStatus(TaskStatus.WAITING);
        task.setExecuteTime(now);
        task.setLastErrorMsg(null);
        task.setErrorMsg(null);
        task.setUpdateTime(now);
        audioTaskMapper.updateById(task);

        String triggerSource = null;
        Long alarmId = null;
        Map<String, Object> providerInfo = new HashMap<>();
        Map<String, Object> stageInfo = new HashMap<>();
        Map<String, Object> fallbackInfo = new HashMap<>();
        boolean autoAlarmEnabled = getConfigBoolean("autoAlarmEnabled", true);
        boolean asrEnabled = getConfigBoolean("asrEnabled", true);
        boolean llmEnabled = getConfigBoolean("llmEnabled", true);
        boolean correctionEnabled = getConfigBoolean("correctionEnabled", true);
        boolean analysisUseCorrectedTranscript = getConfigBoolean("analysisUseCorrectedTranscript", true);
        boolean vadEnabled = getConfigBoolean("vadEnabled", true);
        int riskThreshold = getConfigInt("riskThreshold", 70);

        try {
            validateExecutableAudio(task);
            if (!vadEnabled) {
                log.warn("Task {} execution with vadEnabled=false, backend orchestration will continue and rely on ai-service behavior", taskId);
            }

            SeEnhanceResponseDTO se = seClient.enhance(taskId, task.getOriginalFilePath());
            mergeObservability(providerInfo, stageInfo, fallbackInfo, se.getProviderInfo(), se.getStageInfo(), se.getFallbackInfo());
            task.setEnhancedFilePath(se.getEnhancedFilePath());
            task.setSeStatus(TaskStatus.SUCCESS);
            task.setAsrStatus(TaskStatus.PROCESSING);
            task.setUpdateTime(LocalDateTime.now());
            audioTaskMapper.updateById(task);

            if (!asrEnabled) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "系统配置 asrEnabled=false，已禁用 ASR 执行");
            }
            AsrTranscribeResponseDTO asr = asrClient.transcribe(taskId, task.getEnhancedFilePath());
            mergeObservability(providerInfo, stageInfo, fallbackInfo, asr.getProviderInfo(), asr.getStageInfo(), asr.getFallbackInfo());
            TranscriptCorrectionResult correction = transcriptCorrectionService.correctTranscript(
                taskId,
                asr.getTranscriptText(),
                correctionEnabled,
                llmEnabled
            );
            mergeObservability(providerInfo, stageInfo, fallbackInfo,
                correction.getProviderInfo(), correction.getStageInfo(), correction.getFallbackInfo());
            saveAsrResult(taskId, correction, asr.getConfidence(), asr.getLanguage(),
                resolveProvider(asr.getProviderInfo(), asr.getStageInfo(), "asr"), TASK_TYPE_OFFLINE);
            String analysisTranscript = resolveAnalysisTranscript(correction, analysisUseCorrectedTranscript);

            task.setTranscriptText(analysisTranscript);
            task.setAsrStatus(TaskStatus.SUCCESS);
            task.setLlmStatus(TaskStatus.PROCESSING);
            task.setLastTranscriptTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            audioTaskMapper.updateById(task);

            if (!llmEnabled) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "系统配置 llmEnabled=false，已禁用 LLM 执行");
            }
            LlmAnalyzeResponseDTO llm = llmClient.analyze(taskId, analysisTranscript);
            mergeObservability(providerInfo, stageInfo, fallbackInfo, llm.getProviderInfo(), llm.getStageInfo(), llm.getFallbackInfo());
            LlmAnalysisResult llmRow = saveLlmResult(taskId, llm, providerInfo, stageInfo, fallbackInfo, TASK_TYPE_OFFLINE);
            saveEntityResults(taskId, llm.getEntities());

            RuleDecision decision = evaluateRule(analysisTranscript, llm.getRiskLevel(), llm.getReason(), riskThreshold);
            triggerSource = decision.triggerSource;
            RiskEvent riskEvent = saveRiskEvent(taskId, llmRow.getId(), llm, triggerSource, false);
            if (decision.triggerAlarm && autoAlarmEnabled) {
                alarmId = saveAlarmRecord(task, llmRow.getId(), riskEvent == null ? null : riskEvent.getId(), llm, decision, false);
            } else if (decision.triggerAlarm) {
                log.info("Task {} matched alarm condition but autoAlarmEnabled=false, skip alarm creation", taskId);
            }

            task.setRiskLevel(llm.getRiskLevel());
            task.setLlmStatus(TaskStatus.SUCCESS);
            task.setTaskStatus(TaskStatus.SUCCESS);
            task.setFinishTime(LocalDateTime.now());
            task.setLastErrorMsg(null);
            task.setErrorMsg(null);
            task.setUpdateTime(LocalDateTime.now());
            audioTaskMapper.updateById(task);

            log.info("AI orchestration finished successfully, taskId={}", taskId);
            return ExecuteSummaryVO.builder()
                .taskId(taskId)
                .taskStatus(task.getTaskStatus())
                .seStatus(task.getSeStatus())
                .asrStatus(task.getAsrStatus())
                .llmStatus(task.getLlmStatus())
                .riskLevel(task.getRiskLevel())
                .triggerSource(triggerSource)
                .alarmId(alarmId)
                .message("任务执行完成")
                .providerInfo(providerInfo)
                .stageInfo(stageInfo)
                .fallbackInfo(fallbackInfo)
                .build();
        } catch (Exception ex) {
            log.error("AI orchestration failed, taskId={}", taskId, ex);
            markTaskFailed(task, ex.getMessage());
            if (ex instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ResultCode.SERVER_ERROR, "任务执行失败: " + ex.getMessage());
        }
    }

    @Override
    public AnalysisDetailVO getAnalysisDetail(Long taskId) {
        AudioTask task = requireTask(taskId);

        AsrResult asrResult = asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));

        LlmAnalysisResult llmResult = llmAnalysisResultMapper.selectOne(new LambdaQueryWrapper<LlmAnalysisResult>()
            .eq(LlmAnalysisResult::getTaskId, taskId)
            .orderByDesc(LlmAnalysisResult::getId)
            .last("LIMIT 1"));

        List<EntityResultVO> entityVOs = entityResultMapper.selectList(new LambdaQueryWrapper<EntityResult>()
                .eq(EntityResult::getTaskId, taskId)
                .orderByDesc(EntityResult::getId))
            .stream().map(this::toEntityVO).toList();

        RiskEvent riskEvent = riskEventMapper.selectOne(new LambdaQueryWrapper<RiskEvent>()
            .eq(RiskEvent::getTaskId, taskId)
            .orderByDesc(RiskEvent::getId)
            .last("LIMIT 1"));

        List<AlarmRecordVO> alarmVOs = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getTaskId, taskId)
                .orderByDesc(AlarmRecord::getId))
            .stream().map(this::toAlarmVO).toList();

        Map<String, Object> providerInfo = extractRawNodeMap(llmResult == null ? null : llmResult.getRawResponse(), "providerInfo");
        Map<String, Object> stageInfo = extractRawNodeMap(llmResult == null ? null : llmResult.getRawResponse(), "stageInfo");
        Map<String, Object> fallbackInfo = extractRawNodeMap(llmResult == null ? null : llmResult.getRawResponse(), "fallbackInfo");
        augmentCorrectionObservability(asrResult, providerInfo, stageInfo, fallbackInfo);

        return AnalysisDetailVO.builder()
            .task(toTaskVO(task))
            .asrResult(toAsrVO(asrResult))
            .llmAnalysisResult(toLlmVO(llmResult))
            .entityResults(entityVOs)
            .riskEvent(toRiskEventVO(riskEvent))
            .alarmRecords(alarmVOs)
            .analysisTranscript(preferredTranscript(asrResult, task))
            .analysisUsesCorrectedTranscript(usesCorrectedTranscript(asrResult, task))
            .providerInfo(providerInfo)
            .stageInfo(stageInfo)
            .fallbackInfo(fallbackInfo)
            .build();
    }

    private void validateExecutableAudio(AudioTask task) {
        if (TASK_TYPE_REALTIME.equalsIgnoreCase(task.getTaskType())) {
            return;
        }
        if (task.getOriginalFilePath() == null || task.getOriginalFilePath().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "任务缺少原始音频路径，请重新上传音频或填写有效路径");
        }
        Path path = Path.of(task.getOriginalFilePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "音频文件不存在，请使用本地上传创建任务或填写本机可访问的真实路径");
        }
    }

    @Override
    public RealtimeChunkResultVO ingestRealtimeTranscript(Long channelId,
                                                          Long taskId,
                                                          String sourceSessionId,
                                                          String transcriptText,
                                                          Double confidence,
                                                          String language,
                                                          Integer startTime,
                                                          Integer endTime,
                                                          Map<String, Object> providerInfo,
                                                          Map<String, Object> stageInfo,
                                                          Map<String, Object> fallbackInfo) {
        AudioTask task = requireTask(taskId);
        LocalDateTime now = LocalDateTime.now();
        boolean llmEnabled = getConfigBoolean("llmEnabled", true);
        boolean correctionEnabled = getConfigBoolean("correctionEnabled", true);
        boolean analysisUseCorrectedTranscript = getConfigBoolean("analysisUseCorrectedTranscript", true);

        AsrResult existedAsr = latestAsr(taskId);
        String mergedRawTranscript = appendTranscript(existedAsr == null ? null : existedAsr.getRawTranscript(), transcriptText);
        TranscriptCorrectionResult correction = transcriptCorrectionService.correctTranscript(
            taskId,
            mergedRawTranscript,
            correctionEnabled,
            llmEnabled
        );
        String analysisTranscript = resolveAnalysisTranscript(correction, analysisUseCorrectedTranscript);
        task.setTaskType(TASK_TYPE_REALTIME);
        task.setSourceSessionId(sourceSessionId);
        task.setTranscriptText(analysisTranscript);
        task.setTaskStatus(TaskStatus.PROCESSING);
        task.setSeStatus(TaskStatus.SUCCESS);
        task.setAsrStatus(TaskStatus.SUCCESS);
        task.setLlmStatus(TaskStatus.PROCESSING);
        task.setRiskLevel(null);
        task.setLastTranscriptTime(now);
        task.setUpdateTime(now);
        if (task.getExecuteTime() == null) {
            task.setExecuteTime(now);
        }
        audioTaskMapper.updateById(task);

        saveAsrResult(taskId, correction, confidence, language, resolveProvider(providerInfo, stageInfo, "asr"), TASK_TYPE_REALTIME);

        Map<String, Object> mergedProviderInfo = new HashMap<>();
        Map<String, Object> mergedStageInfo = new HashMap<>();
        Map<String, Object> mergedFallbackInfo = new HashMap<>();
        mergeObservability(mergedProviderInfo, mergedStageInfo, mergedFallbackInfo, providerInfo, stageInfo, fallbackInfo);
        mergeObservability(mergedProviderInfo, mergedStageInfo, mergedFallbackInfo,
            correction.getProviderInfo(), correction.getStageInfo(), correction.getFallbackInfo());

        boolean autoAlarmEnabled = getConfigBoolean("autoAlarmEnabled", true);
        int riskThreshold = getConfigInt("riskThreshold", 70);

        Long alarmId = null;
        String triggerSource = null;
        LlmAnalyzeResponseDTO llm = null;
        LlmAnalysisResult llmRow = null;
        RuleDecision decision = null;

        if (llmEnabled) {
            llm = llmClient.analyze(taskId, analysisTranscript);
            mergeObservability(mergedProviderInfo, mergedStageInfo, mergedFallbackInfo,
                llm.getProviderInfo(), llm.getStageInfo(), llm.getFallbackInfo());
            llmRow = saveLlmResult(taskId, llm, mergedProviderInfo, mergedStageInfo, mergedFallbackInfo, TASK_TYPE_REALTIME);
            saveEntityResults(taskId, llm.getEntities());

            decision = evaluateRule(analysisTranscript, llm.getRiskLevel(), llm.getReason(), riskThreshold);
            triggerSource = decision.triggerSource;
            RiskEvent riskEvent = saveRiskEvent(taskId, llmRow.getId(), llm, triggerSource, true);
            if (decision.triggerAlarm && autoAlarmEnabled) {
                alarmId = saveAlarmRecord(task, llmRow.getId(), riskEvent == null ? null : riskEvent.getId(), llm, decision, true);
            }

            task.setRiskLevel(llm.getRiskLevel());
            task.setLlmStatus(TaskStatus.SUCCESS);
        } else {
            task.setLlmStatus(TaskStatus.WAITING);
        }

        task.setUpdateTime(LocalDateTime.now());
        audioTaskMapper.updateById(task);

        return RealtimeChunkResultVO.builder()
            .segmentId("seg-" + System.currentTimeMillis())
            .taskId(taskId)
            .transcriptText(analysisTranscript)
            .startTime(startTime)
            .endTime(endTime)
            .isFinal(Boolean.TRUE)
            .analysisId(llmRow == null ? null : llmRow.getId())
            .alarmId(alarmId)
            .alarmLevel(alarmId == null || decision == null ? null : decision.alarmLevel)
            .alarmStatus(alarmId == null ? null : AlarmStatus.UNHANDLED)
            .riskLevel(llm == null ? null : llm.getRiskLevel())
            .eventType(llm == null ? null : llm.getEventType())
            .eventSummary(llm == null ? null : llm.getEventSummary())
            .triggerSource(triggerSource)
            .triggerReason(decision == null ? null : decision.triggerReason)
            .providerInfo(mergedProviderInfo)
            .stageInfo(mergedStageInfo)
            .fallbackInfo(mergedFallbackInfo)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private AudioTask requireTask(Long taskId) {
        AudioTask task = audioTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private void saveAsrResult(Long taskId,
                               TranscriptCorrectionResult correction,
                               Double confidence,
                               String language,
                               String provider,
                               String sourceType) {
        AsrResult existed = asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        AsrResult row = existed == null ? new AsrResult() : existed;
        row.setTaskId(taskId);
        row.setTranscriptText(correction.getRawTranscript());
        row.setRawTranscript(correction.getRawTranscript());
        row.setCorrectedTranscript(correction.getCorrectedTranscript());
        row.setCorrectionDiff(correction.getCorrectionDiff());
        row.setCorrectionProvider(correction.getCorrectionProvider());
        row.setCorrectionFallback(correction.isCorrectionFallback() ? 1 : 0);
        row.setConfidence(confidence == null ? null : BigDecimal.valueOf(confidence));
        row.setLanguage(language);
        row.setProvider(provider);
        row.setSourceType(sourceType);
        row.setUpdateTime(now);
        if (existed == null) {
            row.setCreateTime(now);
            asrResultMapper.insert(row);
        } else {
            asrResultMapper.updateById(row);
        }
    }

    private LlmAnalysisResult saveLlmResult(Long taskId,
                                            LlmAnalyzeResponseDTO llm,
                                            Map<String, Object> providerInfo,
                                            Map<String, Object> stageInfo,
                                            Map<String, Object> fallbackInfo,
                                            String sourceType) {
        LlmAnalysisResult existed = llmAnalysisResultMapper.selectOne(new LambdaQueryWrapper<LlmAnalysisResult>()
            .eq(LlmAnalysisResult::getTaskId, taskId)
            .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        LlmAnalysisResult row = existed == null ? new LlmAnalysisResult() : existed;
        row.setTaskId(taskId);
        row.setRiskLevel(llm.getRiskLevel());
        row.setEventType(llm.getEventType());
        row.setEventSummary(llm.getEventSummary());
        row.setReason(llm.getReason());
        row.setProvider(resolveProvider(providerInfo, stageInfo, "llm"));
        row.setSourceType(sourceType);
        row.setRawResponse(toJsonSafely(Map.of(
            "llmResponse", llm,
            "providerInfo", providerInfo == null ? Map.of() : providerInfo,
            "stageInfo", stageInfo == null ? Map.of() : stageInfo,
            "fallbackInfo", fallbackInfo == null ? Map.of() : fallbackInfo
        )));
        row.setUpdateTime(now);

        if (existed == null) {
            row.setCreateTime(now);
            llmAnalysisResultMapper.insert(row);
        } else {
            llmAnalysisResultMapper.updateById(row);
        }
        return row;
    }

    private void saveEntityResults(Long taskId, List<LlmEntityDTO> entities) {
        entityResultMapper.delete(new LambdaQueryWrapper<EntityResult>().eq(EntityResult::getTaskId, taskId));
        if (entities == null || entities.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (LlmEntityDTO e : entities) {
            EntityResult row = new EntityResult();
            row.setTaskId(taskId);
            row.setEntityType(e.getEntityType());
            row.setEntityValue(e.getEntityValue());
            row.setConfidence(e.getConfidence() == null ? null : BigDecimal.valueOf(e.getConfidence()));
            row.setCreateTime(now);
            entityResultMapper.insert(row);
        }
    }

    private RiskEvent saveRiskEvent(Long taskId,
                                    Long analysisId,
                                    LlmAnalyzeResponseDTO llm,
                                    String triggerSource,
                                    boolean updateExisting) {
        LocalDateTime now = LocalDateTime.now();
        RiskEvent event = null;
        if (updateExisting) {
            event = riskEventMapper.selectOne(new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getTaskId, taskId)
                .orderByDesc(RiskEvent::getId)
                .last("LIMIT 1"));
        }
        if (event == null) {
            event = new RiskEvent();
            event.setTaskId(taskId);
            event.setCreateTime(now);
        }
        event.setAnalysisId(analysisId);
        event.setRiskLevel(llm.getRiskLevel());
        event.setEventType(llm.getEventType());
        event.setSummary(llm.getEventSummary());
        event.setSource(triggerSource == null ? TriggerSource.LLM : triggerSource);
        event.setUpdateTime(now);
        if (event.getId() == null) {
            riskEventMapper.insert(event);
        } else {
            riskEventMapper.updateById(event);
        }
        return event;
    }

    private Long saveAlarmRecord(AudioTask task,
                                 Long analysisId,
                                 Long riskEventId,
                                 LlmAnalyzeResponseDTO llm,
                                 RuleDecision decision,
                                 boolean allowReuse) {
        LocalDateTime now = LocalDateTime.now();
        AlarmRecord record = allowReuse ? findOpenAlarm(task.getId()) : null;
        boolean newlyCreated = false;
        if (record == null) {
            record = new AlarmRecord();
            record.setTaskId(task.getId());
            record.setCreateTime(now);
            newlyCreated = true;
        }
        record.setAnalysisId(analysisId);
        record.setRiskEventId(riskEventId);
        record.setChannelId(task.getChannelId());
        record.setAlarmLevel(decision.alarmLevel);
        record.setTriggerSource(decision.triggerSource);
        record.setTriggerReason(decision.triggerReason);
        record.setIsAutoCreated(1);
        record.setHandleRemark(llm == null ? null : llm.getEventSummary());
        record.setUpdateTime(now);
        if (newlyCreated) {
            record.setAlarmStatus(AlarmStatus.UNHANDLED);
            record.setHandleUser(null);
            record.setHandleTime(null);
            alarmRecordMapper.insert(record);
            alarmService.recordAutoCreateAudit(record.getId(), decision.triggerReason);
        } else {
            alarmRecordMapper.updateById(record);
        }
        return record.getId();
    }

    private AlarmRecord findOpenAlarm(Long taskId) {
        return alarmRecordMapper.selectOne(new LambdaQueryWrapper<AlarmRecord>()
            .eq(AlarmRecord::getTaskId, taskId)
            .in(AlarmRecord::getAlarmStatus, List.of(
                AlarmStatus.UNHANDLED,
                AlarmStatus.ACKNOWLEDGED,
                AlarmStatus.PROCESSING
            ))
            .orderByDesc(AlarmRecord::getId)
            .last("LIMIT 1"));
    }

    private String appendTranscript(String existing, String incoming) {
        String next = incoming == null ? "" : incoming.trim();
        if (next.isEmpty()) {
            return existing == null ? "" : existing;
        }
        String base = existing == null ? "" : existing.trim();
        if (base.isEmpty()) {
            return next;
        }
        if (base.endsWith(next)) {
            return base;
        }
        return base + "\n" + next;
    }

    private String resolveAnalysisTranscript(TranscriptCorrectionResult correction, boolean analysisUseCorrectedTranscript) {
        if (correction == null) {
            return "";
        }
        if (analysisUseCorrectedTranscript && correction.getCorrectedTranscript() != null && !correction.getCorrectedTranscript().isBlank()) {
            return correction.getCorrectedTranscript();
        }
        return correction.getRawTranscript();
    }

    private void augmentCorrectionObservability(AsrResult asrResult,
                                                Map<String, Object> providerInfo,
                                                Map<String, Object> stageInfo,
                                                Map<String, Object> fallbackInfo) {
        if (asrResult == null) {
            return;
        }
        if (asrResult.getCorrectionProvider() != null && !asrResult.getCorrectionProvider().isBlank()) {
            providerInfo.put("correction", asrResult.getCorrectionProvider());
            stageInfo.putIfAbsent("correction", Map.of(
                "stage", "correction",
                "effectiveProvider", asrResult.getCorrectionProvider(),
                "fallbackUsed", asrResult.getCorrectionFallback() != null && asrResult.getCorrectionFallback() == 1
            ));
            fallbackInfo.put("correction", Map.of(
                "fallbackUsed", asrResult.getCorrectionFallback() != null && asrResult.getCorrectionFallback() == 1
            ));
        }
    }

    private AsrResult latestAsr(Long taskId) {
        return asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));
    }

    private String preferredTranscript(AsrResult asrResult, AudioTask task) {
        if (asrResult != null && asrResult.getCorrectedTranscript() != null && !asrResult.getCorrectedTranscript().isBlank()) {
            return asrResult.getCorrectedTranscript();
        }
        if (asrResult != null && asrResult.getRawTranscript() != null && !asrResult.getRawTranscript().isBlank()) {
            return asrResult.getRawTranscript();
        }
        return task == null ? null : task.getTranscriptText();
    }

    private Boolean usesCorrectedTranscript(AsrResult asrResult, AudioTask task) {
        if (asrResult == null) {
            return Boolean.FALSE;
        }
        String corrected = asrResult.getCorrectedTranscript();
        if (corrected == null || corrected.isBlank()) {
            return Boolean.FALSE;
        }
        return corrected.equals(task == null ? null : task.getTranscriptText());
    }

    private String resolveProvider(Map<String, Object> providerInfo, Map<String, Object> stageInfo, String stage) {
        if (providerInfo != null && providerInfo.get(stage) != null) {
            return String.valueOf(providerInfo.get(stage));
        }
        if (stageInfo != null) {
            Object stageNode = stageInfo.get(stage);
            if (stageNode instanceof Map<?, ?> stageMap && stageMap.get("effectiveProvider") != null) {
                return String.valueOf(stageMap.get("effectiveProvider"));
            }
        }
        return null;
    }

    private RuleDecision evaluateRule(String transcriptText, String llmRiskLevel, String llmReason, int riskThreshold) {
        String text = transcriptText == null ? "" : transcriptText.toLowerCase(Locale.ROOT);
        List<String> hit = new ArrayList<>();
        for (String keyword : ALERT_KEYWORDS) {
            if (text.contains(keyword)) {
                hit.add(keyword);
            }
        }

        boolean ruleHit = !hit.isEmpty();
        int llmScore = toRiskScore(llmRiskLevel);
        boolean llmHigh = llmScore >= riskThreshold;

        String triggerSource = null;
        if (ruleHit && llmHigh) {
            triggerSource = TriggerSource.RULE_AND_LLM;
        } else if (ruleHit) {
            triggerSource = TriggerSource.RULE;
        } else if (llmHigh) {
            triggerSource = TriggerSource.LLM;
        }

        boolean triggerAlarm = triggerSource != null;
        String alarmLevel = llmHigh ? AlarmLevel.HIGH : (ruleHit ? AlarmLevel.MEDIUM : null);

        StringBuilder reasonBuilder = new StringBuilder();
        if (ruleHit) {
            reasonBuilder.append("规则命中关键词: ").append(String.join(",", hit));
        }
        if (llmHigh) {
            if (!reasonBuilder.isEmpty()) {
                reasonBuilder.append("; ");
            }
            reasonBuilder.append("LLM风险等级命中阈值(").append(riskThreshold).append(")");
            if (llmReason != null && !llmReason.isBlank()) {
                reasonBuilder.append(" (").append(llmReason).append(")");
            }
        }

        return new RuleDecision(triggerAlarm, triggerSource, alarmLevel,
            reasonBuilder.isEmpty() ? "未触发告警" : reasonBuilder.toString());
    }

    private void markTaskFailed(AudioTask task, String message) {
        String msg = message == null ? "未知错误" : message;
        if (msg.length() > 240) {
            msg = msg.substring(0, 240);
        }

        if (TaskStatus.PROCESSING.equals(task.getSeStatus())) {
            task.setSeStatus(TaskStatus.FAILED);
        } else if (TaskStatus.PROCESSING.equals(task.getAsrStatus())) {
            task.setAsrStatus(TaskStatus.FAILED);
        } else if (TaskStatus.PROCESSING.equals(task.getLlmStatus())) {
            task.setLlmStatus(TaskStatus.FAILED);
        }

        task.setTaskStatus(TaskStatus.FAILED);
        task.setErrorMsg(msg);
        task.setLastErrorMsg(msg);
        task.setFinishTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        audioTaskMapper.updateById(task);
    }

    private String toJsonSafely(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void mergeObservability(Map<String, Object> providerInfo,
                                    Map<String, Object> stageInfo,
                                    Map<String, Object> fallbackInfo,
                                    Map<String, Object> providerPart,
                                    Map<String, Object> stagePart,
                                    Map<String, Object> fallbackPart) {
        if (providerPart != null && !providerPart.isEmpty()) {
            providerInfo.putAll(providerPart);
        }
        if (stagePart != null && !stagePart.isEmpty()) {
            stageInfo.putAll(stagePart);
        }
        if (fallbackPart != null && !fallbackPart.isEmpty()) {
            fallbackInfo.putAll(fallbackPart);
        }
    }

    private boolean getConfigBoolean(String key, boolean defaultValue) {
        SystemConfig row = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
            .eq(SystemConfig::getConfigKey, key)
            .last("LIMIT 1"));
        if (row == null || row.getConfigValue() == null) {
            return defaultValue;
        }
        String value = row.getConfigValue().trim().toLowerCase(Locale.ROOT);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value);
    }

    private int getConfigInt(String key, int defaultValue) {
        SystemConfig row = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
            .eq(SystemConfig::getConfigKey, key)
            .last("LIMIT 1"));
        if (row == null || row.getConfigValue() == null || row.getConfigValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(row.getConfigValue().trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid int config, key={}, value={}, use default={}", key, row.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private int toRiskScore(String riskLevel) {
        if (riskLevel == null) {
            return 0;
        }
        if (RiskLevel.HIGH.equalsIgnoreCase(riskLevel)) {
            return 90;
        }
        if (RiskLevel.MEDIUM.equalsIgnoreCase(riskLevel)) {
            return 60;
        }
        if (RiskLevel.LOW.equalsIgnoreCase(riskLevel)) {
            return 30;
        }
        return 0;
    }

    private String toAlarmLevel(String riskLevel, String triggerSource) {
        if (triggerSource == null) {
            return null;
        }
        return RiskLevel.HIGH.equalsIgnoreCase(riskLevel) ? AlarmLevel.HIGH : AlarmLevel.MEDIUM;
    }

    private Map<String, Object> extractRawNodeMap(String raw, String key) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(raw, new TypeReference<>() {
            });
            Object part = payload.get(key);
            if (part instanceof Map<?, ?> map) {
                Map<String, Object> safe = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    safe.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return safe;
            }
        } catch (Exception ex) {
            log.warn("failed to parse observability from llm raw response, key={}, err={}", key, ex.getMessage());
        }
        return Map.of();
    }

    private AudioTaskVO toTaskVO(AudioTask task) {
        return AudioTaskVO.builder()
            .id(task.getId())
            .channelId(task.getChannelId())
            .originalFilePath(task.getOriginalFilePath())
            .enhancedFilePath(task.getEnhancedFilePath())
            .taskType(task.getTaskType())
            .sourceSessionId(task.getSourceSessionId())
            .taskStatus(task.getTaskStatus())
            .seStatus(task.getSeStatus())
            .asrStatus(task.getAsrStatus())
            .llmStatus(task.getLlmStatus())
            .transcriptText(task.getTranscriptText())
            .riskLevel(task.getRiskLevel())
            .duration(task.getDuration())
            .errorMsg(task.getErrorMsg())
            .lastErrorMsg(task.getLastErrorMsg())
            .executeTime(task.getExecuteTime())
            .lastTranscriptTime(task.getLastTranscriptTime())
            .createTime(task.getCreateTime())
            .finishTime(task.getFinishTime())
            .updateTime(task.getUpdateTime())
            .build();
    }

    private AsrResultVO toAsrVO(AsrResult asr) {
        if (asr == null) {
            return null;
        }
        return AsrResultVO.builder()
            .id(asr.getId())
            .taskId(asr.getTaskId())
            .transcriptText(asr.getTranscriptText())
            .rawTranscript(asr.getRawTranscript())
            .correctedTranscript(asr.getCorrectedTranscript())
            .correctionDiff(asr.getCorrectionDiff())
            .correctionProvider(asr.getCorrectionProvider())
            .correctionFallback(asr.getCorrectionFallback() != null && asr.getCorrectionFallback() == 1)
            .confidence(asr.getConfidence())
            .language(asr.getLanguage())
            .provider(asr.getProvider())
            .sourceType(asr.getSourceType())
            .createTime(asr.getCreateTime())
            .updateTime(asr.getUpdateTime())
            .build();
    }

    private LlmAnalysisResultVO toLlmVO(LlmAnalysisResult row) {
        if (row == null) {
            return null;
        }
        return LlmAnalysisResultVO.builder()
            .id(row.getId())
            .taskId(row.getTaskId())
            .riskLevel(row.getRiskLevel())
            .eventType(row.getEventType())
            .eventSummary(row.getEventSummary())
            .reason(row.getReason())
            .provider(row.getProvider())
            .sourceType(row.getSourceType())
            .rawResponse(row.getRawResponse())
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
            .build();
    }

    private EntityResultVO toEntityVO(EntityResult row) {
        return EntityResultVO.builder()
            .id(row.getId())
            .taskId(row.getTaskId())
            .entityType(row.getEntityType())
            .entityValue(row.getEntityValue())
            .confidence(row.getConfidence())
            .createTime(row.getCreateTime())
            .build();
    }

    private RiskEventVO toRiskEventVO(RiskEvent row) {
        if (row == null) {
            return null;
        }
        return RiskEventVO.builder()
            .id(row.getId())
            .taskId(row.getTaskId())
            .analysisId(row.getAnalysisId())
            .riskLevel(row.getRiskLevel())
            .eventType(row.getEventType())
            .summary(row.getSummary())
            .source(row.getSource())
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
            .build();
    }

    private AlarmRecordVO toAlarmVO(AlarmRecord row) {
        return AlarmRecordVO.builder()
            .id(row.getId())
            .taskId(row.getTaskId())
            .analysisId(row.getAnalysisId())
            .riskEventId(row.getRiskEventId())
            .channelId(row.getChannelId())
            .alarmLevel(row.getAlarmLevel())
            .triggerSource(row.getTriggerSource())
            .triggerReason(row.getTriggerReason())
            .alarmStatus(row.getAlarmStatus())
            .isAutoCreated(row.getIsAutoCreated())
            .handleUser(row.getHandleUser())
            .handleTime(row.getHandleTime())
            .handleRemark(row.getHandleRemark())
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
            .build();
    }

    private record RuleDecision(boolean triggerAlarm,
                                String triggerSource,
                                String alarmLevel,
                                String triggerReason) {
    }
}
