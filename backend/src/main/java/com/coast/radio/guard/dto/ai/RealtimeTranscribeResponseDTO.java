package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class RealtimeTranscribeResponseDTO {
    private Boolean success;
    private String segmentId;
    private String transcriptText;
    private Integer startTime;
    private Integer endTime;
    private Boolean isFinal;
    private String language;
    private String message;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
    private String timestamp;
}
