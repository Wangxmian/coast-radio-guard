package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.alarm.AlarmActionRequestDTO;
import com.coast.radio.guard.dto.alarm.ManualAlarmCreateDTO;
import com.coast.radio.guard.service.AlarmService;
import com.coast.radio.guard.vo.alarm.AlarmDetailVO;
import com.coast.radio.guard.vo.audio.AlarmRecordVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @PostMapping("/manual")
    public Result<Map<String, Long>> createManualAlarm(@Valid @RequestBody ManualAlarmCreateDTO dto) {
        Long id = alarmService.createManualAlarm(dto);
        return Result.ok("手动报警创建成功", Map.of("id", id));
    }

    @GetMapping
    public Result<List<AlarmRecordVO>> listAlarms(@RequestParam(required = false) String alarmLevel,
                                                   @RequestParam(required = false) String alarmStatus,
                                                   @RequestParam(required = false) String triggerSource,
                                                   @RequestParam(required = false) Long taskId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false, defaultValue = "1") Long page,
                                                   @RequestParam(required = false, defaultValue = "10000") Long pageSize,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(alarmService.listAlarms(
            alarmLevel, alarmStatus, triggerSource, taskId, startTime, endTime, keyword, page, pageSize
        ));
    }

    @GetMapping("/{id}")
    public Result<AlarmDetailVO> getAlarmDetail(@PathVariable Long id) {
        return Result.ok(alarmService.getAlarmDetail(id));
    }

    @PostMapping("/{id}/ack")
    public Result<Void> ackAlarm(@PathVariable Long id, @RequestBody(required = false) AlarmActionRequestDTO dto) {
        alarmService.acknowledge(id, dto == null ? null : dto.getRemark());
        return Result.ok("告警已签收", null);
    }

    @PostMapping("/{id}/process")
    public Result<Void> processAlarm(@PathVariable Long id, @RequestBody(required = false) AlarmActionRequestDTO dto) {
        alarmService.process(id, dto == null ? null : dto.getRemark());
        return Result.ok("告警处理中", null);
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolveAlarm(@PathVariable Long id, @RequestBody(required = false) AlarmActionRequestDTO dto) {
        alarmService.resolve(id, dto == null ? null : dto.getRemark());
        return Result.ok("告警已处理", null);
    }

    @PostMapping("/{id}/close")
    public Result<Void> closeAlarm(@PathVariable Long id, @RequestBody(required = false) AlarmActionRequestDTO dto) {
        alarmService.close(id, dto == null ? null : dto.getRemark());
        return Result.ok("告警已关闭", null);
    }

    @PostMapping("/{id}/false-alarm")
    public Result<Void> falseAlarm(@PathVariable Long id, @RequestBody(required = false) AlarmActionRequestDTO dto) {
        alarmService.falseAlarm(id, dto == null ? null : dto.getRemark());
        return Result.ok("告警已标记为误报", null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAlarms(@RequestParam(required = false) String alarmLevel,
                                               @RequestParam(required = false) String alarmStatus,
                                               @RequestParam(required = false) String triggerSource,
                                               @RequestParam(required = false) Long taskId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false, defaultValue = "1") Long page,
                                               @RequestParam(required = false, defaultValue = "10000") Long pageSize,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return alarmService.exportAlarms(
            alarmLevel, alarmStatus, triggerSource, taskId, startTime, endTime, keyword, page, pageSize
        );
    }
}
