package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.service.StructuredResultService;
import com.coast.radio.guard.vo.audio.AnalysisDetailVO;
import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.structured.StructuredResultListVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/structured-results")
public class StructuredResultController {

    private final StructuredResultService structuredResultService;

    public StructuredResultController(StructuredResultService structuredResultService) {
        this.structuredResultService = structuredResultService;
    }

    @GetMapping
    public Result<PageResultVO<StructuredResultListVO>> queryStructuredResults(
        @RequestParam(required = false, defaultValue = "1") Long page,
        @RequestParam(required = false, defaultValue = "10") Long pageSize,
        @RequestParam(required = false) Long taskId,
        @RequestParam(required = false) String riskLevel,
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return Result.ok(structuredResultService.queryStructuredResults(
            page, pageSize, taskId, riskLevel, eventType, keyword, startTime, endTime));
    }

    @GetMapping("/{taskId}")
    public Result<AnalysisDetailVO> getStructuredResultDetail(@PathVariable Long taskId) {
        return Result.ok(structuredResultService.getStructuredResultDetail(taskId));
    }

    @GetMapping("/{taskId}/json")
    public Result<Map<String, Object>> getStructuredResultJson(@PathVariable Long taskId) {
        return Result.ok(structuredResultService.getStructuredResultJson(taskId));
    }
}
