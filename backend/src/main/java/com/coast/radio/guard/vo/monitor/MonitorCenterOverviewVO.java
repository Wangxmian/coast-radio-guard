package com.coast.radio.guard.vo.monitor;

import com.coast.radio.guard.vo.structured.StructuredResultListVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MonitorCenterOverviewVO {
    private String systemStatus;
    private RealtimeStatusVO realtimeStatus;
    private Long todayTaskCount;
    private Long todayAlarmCount;
    private Long todayHighRiskEventCount;
    private Long channelCount;
    private List<MonitorRecentTaskVO> recentTasks;
    private List<MonitorRecentAlarmVO> recentAlarms;
    private List<StructuredResultListVO> recentStructuredResults;
    private LocalDateTime generatedAt;
}
