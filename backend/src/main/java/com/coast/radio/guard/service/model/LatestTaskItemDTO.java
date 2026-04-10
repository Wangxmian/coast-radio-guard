package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LatestTaskItemDTO {
    private Long taskId;
    private Long channelId;
    private String channelName;
    private String taskType;
    private String taskStatus;
    private String riskLevel;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
