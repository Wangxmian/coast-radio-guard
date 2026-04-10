package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExecuteSummaryVO {
    private Long taskId;
    private String taskStatus;
    private String seStatus;
    private String asrStatus;
    private String llmStatus;
    private String riskLevel;
    private String triggerSource;
    private Long alarmId;
    private String message;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
