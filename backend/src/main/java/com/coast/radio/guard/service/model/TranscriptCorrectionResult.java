package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TranscriptCorrectionResult {
    private String rawTranscript;
    private String correctedTranscript;
    private String correctionDiff;
    private String correctionProvider;
    private boolean correctionFallback;
    private String fallbackReason;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
