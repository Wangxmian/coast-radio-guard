package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TaskStatusSummaryDTO {
    private LocalDate date;
    private long totalTaskCount;
    private long successTaskCount;
    private long failedTaskCount;
    private long processingTaskCount;
    private long waitingTaskCount;
    private double completionRate;
    private List<LatestTaskItemDTO> failedTasks;
}
