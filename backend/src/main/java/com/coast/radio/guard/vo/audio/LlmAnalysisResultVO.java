package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LlmAnalysisResultVO {
    private Long id;
    private Long taskId;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String reason;
    private String provider;
    private String sourceType;
    private String rawResponse;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
