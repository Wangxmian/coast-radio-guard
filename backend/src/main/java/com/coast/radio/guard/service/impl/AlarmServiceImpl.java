package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.AlarmActionType;
import com.coast.radio.guard.common.constants.AlarmFlow;
import com.coast.radio.guard.common.constants.AlarmLevel;
import com.coast.radio.guard.common.constants.AlarmStatus;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.common.constants.TriggerSource;
import com.coast.radio.guard.dto.alarm.ManualAlarmCreateDTO;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.entity.AsrResult;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.LlmAnalysisResult;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AsrResultMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.LlmAnalysisResultMapper;
import com.coast.radio.guard.service.AlarmAuditLogService;
import com.coast.radio.guard.service.AlarmService;
import com.coast.radio.guard.util.SecurityUtil;
import com.coast.radio.guard.vo.alarm.AlarmDetailVO;
import com.coast.radio.guard.vo.audio.AlarmRecordVO;
import com.coast.radio.guard.vo.audio.AsrResultVO;
import com.coast.radio.guard.vo.audio.AudioTaskVO;
import com.coast.radio.guard.vo.audio.LlmAnalysisResultVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRecordMapper alarmRecordMapper;
    private final AudioTaskMapper audioTaskMapper;
    private final AsrResultMapper asrResultMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;
    private final AlarmAuditLogService alarmAuditLogService;

    public AlarmServiceImpl(AlarmRecordMapper alarmRecordMapper,
                            AudioTaskMapper audioTaskMapper,
                            AsrResultMapper asrResultMapper,
                            LlmAnalysisResultMapper llmAnalysisResultMapper,
                            AlarmAuditLogService alarmAuditLogService) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.audioTaskMapper = audioTaskMapper;
        this.asrResultMapper = asrResultMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
        this.alarmAuditLogService = alarmAuditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManualAlarm(ManualAlarmCreateDTO dto) {
        validateAlarmLevel(dto.getAlarmLevel());

        AudioTask task = null;
        if (dto.getTaskId() != null) {
            task = audioTaskMapper.selectById(dto.getTaskId());
            if (task == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "taskId不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        AlarmRecord row = new AlarmRecord();
        row.setTaskId(dto.getTaskId());
        row.setChannelId(dto.getChannelId() != null ? dto.getChannelId() : (task == null ? null : task.getChannelId()));
        row.setAlarmLevel(dto.getAlarmLevel().toUpperCase(Locale.ROOT));
        row.setTriggerSource(TriggerSource.MANUAL);
        row.setTriggerReason(dto.getTriggerReason());
        row.setAlarmStatus(AlarmStatus.UNHANDLED);
        row.setHandleRemark(dto.getRemark());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        alarmRecordMapper.insert(row);

        alarmAuditLogService.recordAction(
            row.getId(),
            AlarmActionType.MANUAL_CREATE,
            null,
            AlarmStatus.UNHANDLED,
            dto.getRemark(),
            SecurityUtil.currentUsername()
        );

        return row.getId();
    }

    @Override
    public List<AlarmRecordVO> listAlarms(String alarmLevel,
                                          String alarmStatus,
                                          String triggerSource,
                                          Long taskId,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          String keyword,
                                          Long page,
                                          Long pageSize) {
        long safePage = page == null || page < 1 ? 1 : page;
        long safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        String normalizedAlarmLevel = alarmLevel == null ? null : alarmLevel.trim().toUpperCase(Locale.ROOT);
        String normalizedAlarmStatus = alarmStatus == null ? null : alarmStatus.trim().toUpperCase(Locale.ROOT);
        String normalizedTriggerSource = triggerSource == null ? null : triggerSource.trim().toUpperCase(Locale.ROOT);

        LambdaQueryWrapper<AlarmRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(normalizedAlarmLevel != null && !normalizedAlarmLevel.isBlank(), AlarmRecord::getAlarmLevel, normalizedAlarmLevel)
            .eq(normalizedAlarmStatus != null && !normalizedAlarmStatus.isBlank(), AlarmRecord::getAlarmStatus, normalizedAlarmStatus)
            .eq(normalizedTriggerSource != null && !normalizedTriggerSource.isBlank(), AlarmRecord::getTriggerSource, normalizedTriggerSource)
            .eq(taskId != null, AlarmRecord::getTaskId, taskId)
            .ge(startTime != null, AlarmRecord::getCreateTime, startTime)
            .le(endTime != null, AlarmRecord::getCreateTime, endTime)
            .and(keyword != null && !keyword.isBlank(), w -> w
                .like(AlarmRecord::getTriggerReason, keyword)
                .or()
                .like(AlarmRecord::getHandleRemark, keyword)
                .or()
                .like(AlarmRecord::getHandleUser, keyword))
            .orderByDesc(AlarmRecord::getId);

        List<AlarmRecordVO> all = alarmRecordMapper.selectList(qw).stream().map(this::toVO).toList();
        int from = (int) ((safePage - 1) * safePageSize);
        if (from >= all.size()) {
            return List.of();
        }
        int to = (int) Math.min(from + safePageSize, all.size());
        return all.subList(from, to);
    }

    @Override
    public AlarmDetailVO getAlarmDetail(Long id) {
        AlarmRecord alarm = requireById(id);
        AudioTask task = alarm.getTaskId() == null ? null : audioTaskMapper.selectById(alarm.getTaskId());
        AsrResult asrResult = latestAsr(alarm.getTaskId());
        LlmAnalysisResult llmResult = latestLlm(alarm.getTaskId(), alarm.getAnalysisId());
        return AlarmDetailVO.builder()
            .alarm(toVO(alarm))
            .task(toTaskVO(task))
            .asrResult(toAsrVO(asrResult))
            .llmAnalysisResult(toLlmVO(llmResult))
            .auditLogs(alarmAuditLogService.listByAlarmId(id))
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledge(Long id, String remark) {
        changeStatus(id, AlarmStatus.ACKNOWLEDGED, AlarmActionType.ACK, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(Long id, String remark) {
        changeStatus(id, AlarmStatus.PROCESSING, AlarmActionType.PROCESS, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long id, String remark) {
        changeStatus(id, AlarmStatus.RESOLVED, AlarmActionType.RESOLVE, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String remark) {
        changeStatus(id, AlarmStatus.CLOSED, AlarmActionType.CLOSE, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void falseAlarm(Long id, String remark) {
        changeStatus(id, AlarmStatus.FALSE_ALARM, AlarmActionType.FALSE_ALARM, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAutoCreateAudit(Long alarmId, String remark) {
        alarmAuditLogService.recordAction(
            alarmId,
            AlarmActionType.AUTO_CREATE,
            null,
            AlarmStatus.UNHANDLED,
            remark,
            "SYSTEM"
        );
    }

    @Override
    public ResponseEntity<byte[]> exportAlarms(String alarmLevel,
                                               String alarmStatus,
                                               String triggerSource,
                                               Long taskId,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime,
                                               String keyword,
                                               Long page,
                                               Long pageSize) {
        List<AlarmRecordVO> rows = listAlarms(
            alarmLevel, alarmStatus, triggerSource, taskId, startTime, endTime, keyword, page, pageSize
        );

        StringBuilder sb = new StringBuilder();
        sb.append("id,taskId,channelId,alarmLevel,triggerSource,triggerReason,alarmStatus,handleUser,handleTime,latestRemark,createTime,updateTime\n");
        for (AlarmRecordVO row : rows) {
            sb.append(csv(row.getId())).append(',')
                .append(csv(row.getTaskId())).append(',')
                .append(csv(row.getChannelId())).append(',')
                .append(csv(row.getAlarmLevel())).append(',')
                .append(csv(row.getTriggerSource())).append(',')
                .append(csv(row.getTriggerReason())).append(',')
                .append(csv(row.getAlarmStatus())).append(',')
                .append(csv(row.getHandleUser())).append(',')
                .append(csv(row.getHandleTime())).append(',')
                .append(csv(row.getLatestRemark())).append(',')
                .append(csv(row.getCreateTime())).append(',')
                .append(csv(row.getUpdateTime()))
                .append('\n');
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "alarms_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private void changeStatus(Long id, String targetStatus, String actionType, String remark) {
        AlarmRecord alarm = requireById(id);
        String fromStatus = alarm.getAlarmStatus();
        if (!AlarmFlow.canTransition(fromStatus, targetStatus)) {
            throw new BusinessException(
                ResultCode.BAD_REQUEST,
                "非法状态流转: " + fromStatus + " -> " + targetStatus
            );
        }

        String operator = SecurityUtil.currentUsername();
        LocalDateTime now = LocalDateTime.now();
        alarm.setAlarmStatus(targetStatus);
        alarm.setHandleUser(operator);
        alarm.setHandleTime(now);
        alarm.setHandleRemark(remark);
        alarm.setUpdateTime(now);
        alarmRecordMapper.updateById(alarm);

        alarmAuditLogService.recordAction(
            alarm.getId(),
            actionType,
            fromStatus,
            targetStatus,
            remark,
            operator
        );
    }

    private AlarmRecord requireById(Long id) {
        AlarmRecord row = alarmRecordMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        return row;
    }

    private void validateAlarmLevel(String value) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "alarmLevel不能为空");
        }
        String level = value.toUpperCase(Locale.ROOT);
        if (!AlarmLevel.HIGH.equals(level) && !AlarmLevel.MEDIUM.equals(level) && !AlarmLevel.LOW.equals(level)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "alarmLevel仅支持HIGH/MEDIUM/LOW");
        }
    }

    private AlarmRecordVO toVO(AlarmRecord row) {
        AudioTask task = row.getTaskId() == null ? null : audioTaskMapper.selectById(row.getTaskId());
        AsrResult asrResult = latestAsr(row.getTaskId());
        LlmAnalysisResult llmResult = latestLlm(row.getTaskId(), row.getAnalysisId());
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
            .riskLevel(llmResult == null ? (task == null ? null : task.getRiskLevel()) : llmResult.getRiskLevel())
            .eventType(llmResult == null ? null : llmResult.getEventType())
            .eventSummary(llmResult == null ? null : llmResult.getEventSummary())
            .transcriptText(preferredTranscript(asrResult, task))
            .handleUser(row.getHandleUser())
            .handleTime(row.getHandleTime())
            .handleRemark(row.getHandleRemark())
            .latestRemark(row.getHandleRemark())
            .inWorkflow(!AlarmStatus.UNHANDLED.equals(row.getAlarmStatus()))
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
            .build();
    }

    private AsrResult latestAsr(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));
    }

    private LlmAnalysisResult latestLlm(Long taskId, Long analysisId) {
        if (analysisId != null) {
            return llmAnalysisResultMapper.selectById(analysisId);
        }
        if (taskId == null) {
            return null;
        }
        return llmAnalysisResultMapper.selectOne(new LambdaQueryWrapper<LlmAnalysisResult>()
            .eq(LlmAnalysisResult::getTaskId, taskId)
            .orderByDesc(LlmAnalysisResult::getId)
            .last("LIMIT 1"));
    }

    private AudioTaskVO toTaskVO(AudioTask task) {
        if (task == null) {
            return null;
        }
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

    private AsrResultVO toAsrVO(AsrResult row) {
        if (row == null) {
            return null;
        }
        return AsrResultVO.builder()
            .id(row.getId())
            .taskId(row.getTaskId())
            .transcriptText(row.getTranscriptText())
            .rawTranscript(row.getRawTranscript())
            .correctedTranscript(row.getCorrectedTranscript())
            .correctionDiff(row.getCorrectionDiff())
            .correctionProvider(row.getCorrectionProvider())
            .correctionFallback(row.getCorrectionFallback() != null && row.getCorrectionFallback() == 1)
            .confidence(row.getConfidence())
            .language(row.getLanguage())
            .provider(row.getProvider())
            .sourceType(row.getSourceType())
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
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

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
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
}
