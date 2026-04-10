package com.coast.radio.guard.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitorRecentAlarmVO {
    private Long id;
    private Long taskId;
    private Long analysisId;
    private Long channelId;
    private String alarmLevel;
    private String alarmStatus;
    private String triggerSource;
    private String triggerReason;
    private LocalDateTime createTime;
}
