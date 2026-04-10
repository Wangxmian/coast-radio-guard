package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LlmAnalyzeResponseDTO {
    private Boolean success;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String reason;
    private List<LlmEntityDTO> entities;
    private String message;
    private String rawResponse;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
