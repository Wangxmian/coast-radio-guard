package com.coast.radio.guard.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResultResponse {
    private String taskNo;
    private String fileName;
    private String status;
    private String transcript;
    private String riskLevel;
    private String summary;
    private LocalDateTime createdAt;
}
