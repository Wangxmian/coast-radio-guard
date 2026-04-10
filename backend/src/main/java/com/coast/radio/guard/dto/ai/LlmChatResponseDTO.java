package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class LlmChatResponseDTO {
    private Boolean success;
    private String answer;
    private String message;
    private String rawResponse;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}

