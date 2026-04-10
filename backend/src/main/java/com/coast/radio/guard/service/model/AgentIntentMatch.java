package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentIntentMatch {
    private AgentIntentType intent;
    private double confidence;
    private String rule;
}
