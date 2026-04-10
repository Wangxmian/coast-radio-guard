package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class LlmCorrectionResponseDTO {
    private Boolean success;
    private String correctedTranscript;
    private String message;
    private String rawResponse;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
