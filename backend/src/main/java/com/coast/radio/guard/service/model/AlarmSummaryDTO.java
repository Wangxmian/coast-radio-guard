package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AlarmSummaryDTO {
    private LocalDate date;
    private long totalAlarmCount;
    private long highRiskAlarmCount;
    private long unhandledAlarmCount;
    private long totalTaskCount;
    private long successTaskCount;
    private long failedTaskCount;
    private LatestAlarmItemDTO latestHighRiskAlarm;
    private List<LatestAlarmItemDTO> recentUnhandledAlarms;
}
