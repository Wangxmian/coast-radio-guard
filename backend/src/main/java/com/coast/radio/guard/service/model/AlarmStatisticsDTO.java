package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlarmStatisticsDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalAlarmCount;
    private long highRiskAlarmCount;
    private double highRiskRatio;
    private LatestAlarmItemDTO latestHighRiskAlarm;
}
