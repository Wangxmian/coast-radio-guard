package com.coast.radio.guard.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitorRecentTaskVO {
    private Long taskId;
    private Long channelId;
    private String taskStatus;
    private String riskLevel;
    private LocalDateTime createTime;
}
