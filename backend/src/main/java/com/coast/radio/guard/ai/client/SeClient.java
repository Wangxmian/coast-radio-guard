package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.dto.ai.SeEnhanceResponseDTO;

public interface SeClient {
    SeEnhanceResponseDTO enhance(Long taskId, String originalFilePath);
}
