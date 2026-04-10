package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.entity.AsrResult;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.EntityResult;
import com.coast.radio.guard.entity.LlmAnalysisResult;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AsrResultMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.EntityResultMapper;
import com.coast.radio.guard.mapper.LlmAnalysisResultMapper;
import com.coast.radio.guard.service.AiOrchestrationService;
import com.coast.radio.guard.service.StructuredResultService;
import com.coast.radio.guard.vo.audio.AnalysisDetailVO;
import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.structured.StructuredResultListVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StructuredResultServiceImpl implements StructuredResultService {

    private final AudioTaskMapper audioTaskMapper;
    private final AsrResultMapper asrResultMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;
    private final EntityResultMapper entityResultMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final AiOrchestrationService aiOrchestrationService;

    public StructuredResultServiceImpl(AudioTaskMapper audioTaskMapper,
                                       AsrResultMapper asrResultMapper,
                                       LlmAnalysisResultMapper llmAnalysisResultMapper,
                                       EntityResultMapper entityResultMapper,
                                       AlarmRecordMapper alarmRecordMapper,
                                       AiOrchestrationService aiOrchestrationService) {
        this.audioTaskMapper = audioTaskMapper;
        this.asrResultMapper = asrResultMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
        this.entityResultMapper = entityResultMapper;
        this.alarmRecordMapper = alarmRecordMapper;
        this.aiOrchestrationService = aiOrchestrationService;
    }

    @Override
    public PageResultVO<StructuredResultListVO> queryStructuredResults(Long page,
                                                                        Long pageSize,
                                                                        Long taskId,
                                                                        String riskLevel,
                                                                        String eventType,
                                                                        String keyword,
                                                                        LocalDateTime startTime,
                                                                        LocalDateTime endTime) {
        long safePage = page == null || page < 1 ? 1 : page;
        long safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        String normalizedRiskLevel = riskLevel == null ? null : riskLevel.trim().toUpperCase(Locale.ROOT);

        LambdaQueryWrapper<AudioTask> taskQw = new LambdaQueryWrapper<>();
        taskQw.eq(taskId != null, AudioTask::getId, taskId)
            .eq(normalizedRiskLevel != null && !normalizedRiskLevel.isBlank(), AudioTask::getRiskLevel, normalizedRiskLevel)
            .ge(startTime != null, AudioTask::getCreateTime, startTime)
            .le(endTime != null, AudioTask::getCreateTime, endTime)
            .orderByDesc(AudioTask::getId);

        List<AudioTask> tasks = audioTaskMapper.selectList(taskQw);
        List<StructuredResultListVO> merged = tasks.stream()
            .map(this::toStructuredResult)
            .filter(row -> matchEventType(row, eventType))
            .filter(row -> matchKeyword(row, keyword))
            .toList();

        int from = (int) ((safePage - 1) * safePageSize);
        if (from >= merged.size()) {
            return PageResultVO.<StructuredResultListVO>builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(merged.size())
                .records(List.of())
                .build();
        }

        int to = (int) Math.min(from + safePageSize, merged.size());
        return PageResultVO.<StructuredResultListVO>builder()
            .page(safePage)
            .pageSize(safePageSize)
            .total(merged.size())
            .records(merged.subList(from, to))
            .build();
    }

    @Override
    public AnalysisDetailVO getStructuredResultDetail(Long taskId) {
        return aiOrchestrationService.getAnalysisDetail(taskId);
    }

    @Override
    public Map<String, Object> getStructuredResultJson(Long taskId) {
        AnalysisDetailVO detail = getStructuredResultDetail(taskId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", detail.getTask());
        payload.put("asrResult", detail.getAsrResult());
        payload.put("llmAnalysisResult", detail.getLlmAnalysisResult());
        payload.put("entityResults", detail.getEntityResults());
        payload.put("riskEvent", detail.getRiskEvent());
        payload.put("alarmRecords", detail.getAlarmRecords());
        payload.put("analysisTranscript", detail.getAnalysisTranscript());
        payload.put("analysisUsesCorrectedTranscript", detail.getAnalysisUsesCorrectedTranscript());
        payload.put("providerInfo", detail.getProviderInfo());
        payload.put("stageInfo", detail.getStageInfo());
        payload.put("fallbackInfo", detail.getFallbackInfo());
        payload.put("generatedAt", LocalDateTime.now());
        return payload;
    }

    private StructuredResultListVO toStructuredResult(AudioTask task) {
        AsrResult asr = latestAsr(task.getId());
        LlmAnalysisResult llm = latestLlm(task.getId());
        AlarmRecord alarm = latestAlarm(task.getId());
        int entitiesCount = countEntities(task.getId());

        String transcript = preferredTranscript(asr, task);
        String resolvedRiskLevel = llm == null ? task.getRiskLevel() : llm.getRiskLevel();

        return StructuredResultListVO.builder()
            .taskId(task.getId())
            .analysisId(llm == null ? null : llm.getId())
            .alarmId(alarm == null ? null : alarm.getId())
            .channelId(task.getChannelId())
            .transcriptText(transcript)
            .rawTranscript(asr == null ? task.getTranscriptText() : asr.getRawTranscript())
            .correctedTranscript(asr == null ? task.getTranscriptText() : asr.getCorrectedTranscript())
            .correctionDiff(asr == null ? null : asr.getCorrectionDiff())
            .correctionFallback(asr != null && asr.getCorrectionFallback() != null && asr.getCorrectionFallback() == 1)
            .riskLevel(resolvedRiskLevel)
            .eventType(llm == null ? null : llm.getEventType())
            .eventSummary(llm == null ? null : llm.getEventSummary())
            .alarmStatus(alarm == null ? null : alarm.getAlarmStatus())
            .entitiesCount(entitiesCount)
            .createTime(task.getCreateTime())
            .build();
    }

    private AsrResult latestAsr(Long taskId) {
        return asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));
    }

    private LlmAnalysisResult latestLlm(Long taskId) {
        return llmAnalysisResultMapper.selectOne(new LambdaQueryWrapper<LlmAnalysisResult>()
            .eq(LlmAnalysisResult::getTaskId, taskId)
            .orderByDesc(LlmAnalysisResult::getId)
            .last("LIMIT 1"));
    }

    private int countEntities(Long taskId) {
        Long count = entityResultMapper.selectCount(new LambdaQueryWrapper<EntityResult>()
            .eq(EntityResult::getTaskId, taskId));
        return count == null ? 0 : count.intValue();
    }

    private AlarmRecord latestAlarm(Long taskId) {
        return alarmRecordMapper.selectOne(new LambdaQueryWrapper<AlarmRecord>()
            .eq(AlarmRecord::getTaskId, taskId)
            .orderByDesc(AlarmRecord::getId)
            .last("LIMIT 1"));
    }

    private boolean matchEventType(StructuredResultListVO row, String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return true;
        }
        return row.getEventType() != null && eventType.equalsIgnoreCase(row.getEventType());
    }

    private boolean matchKeyword(StructuredResultListVO row, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String k = keyword.toLowerCase(Locale.ROOT);
        if (contains(row.getTaskId(), k)
            || contains(row.getTranscriptText(), k)
            || contains(row.getRiskLevel(), k)
            || contains(row.getEventType(), k)
            || contains(row.getEventSummary(), k)) {
            return true;
        }

        Long hit = entityResultMapper.selectCount(new LambdaQueryWrapper<EntityResult>()
            .eq(EntityResult::getTaskId, row.getTaskId())
            .and(w -> w.like(EntityResult::getEntityType, keyword)
                .or()
                .like(EntityResult::getEntityValue, keyword)));
        return hit != null && hit > 0;
    }

    private boolean contains(Object value, String keyword) {
        if (value == null) {
            return false;
        }
        return String.valueOf(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String preferredTranscript(AsrResult asr, AudioTask task) {
        if (asr != null && asr.getCorrectedTranscript() != null && !asr.getCorrectedTranscript().isBlank()) {
            return asr.getCorrectedTranscript();
        }
        if (asr != null && asr.getRawTranscript() != null && !asr.getRawTranscript().isBlank()) {
            return asr.getRawTranscript();
        }
        return task == null ? null : task.getTranscriptText();
    }

    private String crop(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
