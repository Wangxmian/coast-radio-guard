package com.coast.radio.guard.dto;

import lombok.Data;

import java.util.List;

@Data
public class LlmResponse {
    private String riskLevel;
    private List<String> keywords;
    private String summary;
    private String suggestion;
}
