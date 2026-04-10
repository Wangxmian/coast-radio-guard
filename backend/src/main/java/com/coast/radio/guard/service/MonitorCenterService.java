package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.monitor.MonitorCenterOverviewVO;
import com.coast.radio.guard.vo.monitor.RealtimeStatusVO;

public interface MonitorCenterService {

    MonitorCenterOverviewVO getOverview();

    RealtimeStatusVO getRealtimeStatus();
}
