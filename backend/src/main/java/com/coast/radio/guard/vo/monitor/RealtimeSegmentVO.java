package com.coast.radio.guard.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class RealtimeSegmentVO {
    private String segmentId;
    private Long taskId;
    private String transcriptText;
    private Integer startTime;
    private Integer endTime;
    private Boolean isFinal;
    private Long analysisId;
    private Long alarmId;
    private String riskLevel;
    private String eventType;
    private Map<String, Object> providerInfo;
    private LocalDateTime timestamp;
}
