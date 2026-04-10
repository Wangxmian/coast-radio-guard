package com.coast.radio.guard.dto.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RealtimeSessionStatusDTO {
    private Boolean success;
    private String sessionId;
    private Long channelId;
    private String mode;
    private String status;
    private String transcriptText;
    private List<Map<String, Object>> segments;
    private Integer chunkCount;
    private String message;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
    private String timestamp;
    private String startedAt;
    private String stoppedAt;
    private String capabilityNote;
}
