package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.dto.ai.RealtimeTranscribeResponseDTO;

public interface RealtimeAsrClient {

    RealtimeTranscribeResponseDTO transcribeChunk(byte[] bytes,
                                                  String fileName,
                                                  Long taskId,
                                                  Integer startTime,
                                                  Integer endTime);
}
