package com.coast.radio.guard.vo.structured;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StructuredResultListVO {
    private Long taskId;
    private Long analysisId;
    private Long alarmId;
    private Long channelId;
    private String transcriptText;
    private String rawTranscript;
    private String correctedTranscript;
    private String correctionDiff;
    private Boolean correctionFallback;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String alarmStatus;
    private Integer entitiesCount;
    private LocalDateTime createTime;
}
