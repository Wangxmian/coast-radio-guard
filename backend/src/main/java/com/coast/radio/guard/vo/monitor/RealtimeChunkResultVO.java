package com.coast.radio.guard.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class RealtimeChunkResultVO {
    private String segmentId;
    private Long taskId;
    private String transcriptText;
    private Integer startTime;
    private Integer endTime;
    private Boolean isFinal;
    private Long analysisId;
    private Long alarmId;
    private String alarmLevel;
    private String alarmStatus;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String triggerSource;
    private String triggerReason;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
    private LocalDateTime timestamp;
}
