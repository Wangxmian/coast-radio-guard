package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.config.SystemConfigBatchUpdateDTO;
import com.coast.radio.guard.service.SystemConfigService;
import com.coast.radio.guard.vo.config.SystemConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public Result<List<SystemConfigVO>> listConfigs() {
        return Result.ok(systemConfigService.listConfigs());
    }

    @GetMapping("/grouped")
    public Result<Map<String, List<SystemConfigVO>>> listGroupedConfigs() {
        return Result.ok(systemConfigService.listGroupedConfigs());
    }

    @PutMapping
    public Result<Void> updateConfigs(@Valid @RequestBody SystemConfigBatchUpdateDTO dto) {
        systemConfigService.updateConfigs(dto);
        return Result.ok("配置更新成功", null);
    }
}
