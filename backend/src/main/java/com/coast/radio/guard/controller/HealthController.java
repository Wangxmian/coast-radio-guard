package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
            "service", "coast-radio-guard-backend",
            "status", "UP",
            "time", OffsetDateTime.now().toString()
        ));
    }
}
