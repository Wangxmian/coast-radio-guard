package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AlarmTrendSummaryDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalAlarmCount;
    private long totalHighRiskCount;
    private LocalDate peakAlarmDate;
    private long peakAlarmCount;
    private List<AlarmTrendPointDTO> points;
}
