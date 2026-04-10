package com.coast.radio.guard.vo.history;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryRecordVO {
    private Long taskId;
    private Long channelId;
    private Long analysisId;
    private Long alarmId;
    private String taskType;
    private String taskStatus;
    private LocalDateTime createTime;
    private String transcriptText;
    private String rawTranscript;
    private String correctedTranscript;
    private String correctionDiff;
    private Boolean correctionFallback;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String alarmLevel;
    private String alarmStatus;
    private String triggerReason;
    private Boolean hasAlarm;
}
