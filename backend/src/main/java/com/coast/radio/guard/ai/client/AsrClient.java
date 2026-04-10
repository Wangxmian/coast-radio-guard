package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.dto.ai.AsrTranscribeResponseDTO;

public interface AsrClient {
    AsrTranscribeResponseDTO transcribe(Long taskId, String enhancedFilePath);
}
