package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.service.MonitorCenterService;
import com.coast.radio.guard.vo.monitor.MonitorCenterOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor-center")
public class MonitorCenterController {

    private final MonitorCenterService monitorCenterService;

    public MonitorCenterController(MonitorCenterService monitorCenterService) {
        this.monitorCenterService = monitorCenterService;
    }

    @GetMapping("/overview")
    public Result<MonitorCenterOverviewVO> getOverview() {
        return Result.ok(monitorCenterService.getOverview());
    }
}
