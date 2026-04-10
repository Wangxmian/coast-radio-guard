package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlarmListDTO {
    private String scope;
    private long totalCount;
    private List<LatestAlarmItemDTO> items;
}
