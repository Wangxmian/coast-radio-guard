package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class AsrTranscribeResponseDTO {
    private Boolean success;
    private String transcriptText;
    private Double confidence;
    private String language;
    private String message;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
