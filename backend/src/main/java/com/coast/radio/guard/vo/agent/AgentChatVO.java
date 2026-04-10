package com.coast.radio.guard.vo.agent;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AgentChatVO {
    private Boolean success;
    private String answer;
    private String intent;
    private Double confidence;
    private String source;
    private Boolean fallback;
    private Object structuredData;
    private String mode;
    private Map<String, Object> providerInfo;
    private Map<String, Object> fallbackInfo;
}
