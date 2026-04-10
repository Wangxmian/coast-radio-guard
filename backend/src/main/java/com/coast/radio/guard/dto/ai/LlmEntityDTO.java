package com.coast.radio.guard.dto.ai;

import lombok.Data;

@Data
public class LlmEntityDTO {
    private String entityType;
    private String entityValue;
    private Double confidence;
}
