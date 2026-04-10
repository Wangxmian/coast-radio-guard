package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlarmRecordVO {
    private Long id;
    private Long taskId;
    private Long analysisId;
    private Long riskEventId;
    private Long channelId;
    private String alarmLevel;
    private String triggerSource;
    private String triggerReason;
    private String alarmStatus;
    private Integer isAutoCreated;
    private String riskLevel;
    private String eventType;
    private String eventSummary;
    private String transcriptText;
    private String handleUser;
    private LocalDateTime handleTime;
    private String handleRemark;
    private String latestRemark;
    private Boolean inWorkflow;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
