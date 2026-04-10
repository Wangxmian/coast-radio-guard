package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.dto.ai.LlmAnalyzeResponseDTO;
import com.coast.radio.guard.dto.ai.LlmChatResponseDTO;
import com.coast.radio.guard.dto.ai.LlmCorrectionResponseDTO;

public interface LlmClient {
    LlmAnalyzeResponseDTO analyze(Long taskId, String transcriptText);
    LlmCorrectionResponseDTO correctTranscript(Long taskId, String transcriptText);
    LlmChatResponseDTO chat(String message);
    LlmChatResponseDTO formatAnswer(String intent, String question, Object structuredData, String fallbackAnswer);
}
