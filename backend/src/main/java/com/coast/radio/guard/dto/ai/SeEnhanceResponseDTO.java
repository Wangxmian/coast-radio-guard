package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class SeEnhanceResponseDTO {
    private Boolean success;
    private String enhancedFilePath;
    private String message;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
