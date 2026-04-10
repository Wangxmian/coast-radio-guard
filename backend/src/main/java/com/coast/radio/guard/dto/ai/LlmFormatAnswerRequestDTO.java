package com.coast.radio.guard.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmFormatAnswerRequestDTO {
    private String intent;
    private String question;
    private Object structuredData;
    private String fallbackAnswer;
}
