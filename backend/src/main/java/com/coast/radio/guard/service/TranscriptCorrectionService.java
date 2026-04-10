package com.coast.radio.guard.service;

import com.coast.radio.guard.service.model.TranscriptCorrectionResult;

public interface TranscriptCorrectionService {
    TranscriptCorrectionResult correctTranscript(Long taskId, String rawTranscript, boolean correctionEnabled, boolean llmEnabled);
}
