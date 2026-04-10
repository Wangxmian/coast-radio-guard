package com.coast.radio.guard.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RealtimeStatusVO {
    private String listeningStatus;
    private String streamStatus;
    private String sessionId;
    private Long currentChannel;
    private Long currentTaskId;
    private String currentTranscript;
    private String latestRiskLevel;
    private String latestEventType;
    private Long latestAlarmId;
    private String latestAlarmLevel;
    private String latestAlarmStatus;
    private String latestAlarmReason;
    private List<RealtimeSegmentVO> recentSegments;
    private Map<String, Object> providerInfo;
    private String message;
    private String note;
    private String capabilityNote;
    private LocalDateTime lastUpdatedAt;
}
