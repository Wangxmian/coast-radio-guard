package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AlarmTrendPointDTO {
    private LocalDate date;
    private long alarmCount;
    private long highRiskCount;
}
