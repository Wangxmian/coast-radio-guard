package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LatestAlarmItemDTO {
    private Long alarmId;
    private Long taskId;
    private Long channelId;
    private String channelName;
    private String eventType;
    private String eventSummary;
    private String riskLevel;
    private String alarmStatus;
    private String triggerReason;
    private LocalDateTime createTime;
}
