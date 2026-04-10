package com.coast.radio.guard.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmAnalyzeRequestDTO {
    private Long taskId;
    private String transcriptText;
}
