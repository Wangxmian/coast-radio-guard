package com.coast.radio.guard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String baseUrl = "http://127.0.0.1:8000";
    private String sePath = "/se/enhance";
    private String asrPath = "/asr/transcribe";
    private String realtimeAsrPath = "/realtime/transcribe";
    private String llmPath = "/llm/analyze";
    private String llmCorrectionPath = "/llm/correct";
    private String llmChatPath = "/llm/chat";
    private String llmFormatAnswerPath = "/llm/format-answer";
    private int timeoutMillis = 8000;
}
