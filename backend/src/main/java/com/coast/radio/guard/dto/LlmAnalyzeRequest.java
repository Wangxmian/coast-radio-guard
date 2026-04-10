package com.coast.radio.guard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmAnalyzeRequest {
    private String transcript;
}
