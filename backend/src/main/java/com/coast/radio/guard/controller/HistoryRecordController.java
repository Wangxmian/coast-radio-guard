package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.service.HistoryRecordService;
import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.history.HistoryRecordVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/history-records")
public class HistoryRecordController {

    private final HistoryRecordService historyRecordService;

    public HistoryRecordController(HistoryRecordService historyRecordService) {
        this.historyRecordService = historyRecordService;
    }

    @GetMapping
    public Result<PageResultVO<HistoryRecordVO>> queryHistoryRecords(
        @RequestParam(required = false, defaultValue = "1") Long page,
        @RequestParam(required = false, defaultValue = "10") Long pageSize,
        @RequestParam(required = false) Long taskId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String taskStatus,
        @RequestParam(required = false) String riskLevel,
        @RequestParam(required = false) Long channelId,
        @RequestParam(required = false) String alarmLevel
    ) {
        return Result.ok(historyRecordService.queryHistoryRecords(
            page, pageSize, taskId, startTime, endTime, keyword, taskStatus, riskLevel, channelId, alarmLevel));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportHistoryRecords(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @RequestParam(required = false) Long taskId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String taskStatus,
        @RequestParam(required = false) String riskLevel,
        @RequestParam(required = false) String alarmLevel
    ) {
        return historyRecordService.exportHistoryRecords(startTime, endTime, taskId, keyword, taskStatus, riskLevel, alarmLevel);
    }
}
