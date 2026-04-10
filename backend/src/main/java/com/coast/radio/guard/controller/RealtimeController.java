package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.realtime.RealtimeStartRequestDTO;
import com.coast.radio.guard.service.RealtimeService;
import com.coast.radio.guard.vo.monitor.RealtimeChunkResultVO;
import com.coast.radio.guard.vo.monitor.RealtimeStatusVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {

    private final RealtimeService realtimeService;

    public RealtimeController(RealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @PostMapping("/start")
    public Result<RealtimeStatusVO> start(@RequestBody(required = false) RealtimeStartRequestDTO dto) {
        Long channelId = dto == null ? null : dto.getChannelId();
        String mode = dto == null ? null : dto.getMode();
        return Result.ok("实时监听已启动", realtimeService.start(channelId, mode));
    }

    @PostMapping("/stop")
    public Result<RealtimeStatusVO> stop() {
        return Result.ok("实时监听已停止", realtimeService.stop());
    }

    @PostMapping("/chunk")
    public Result<RealtimeChunkResultVO> chunk(@RequestParam("audio") MultipartFile audio,
                                               @RequestParam(required = false) Long taskId,
                                               @RequestParam(required = false) Integer startTime,
                                               @RequestParam(required = false) Integer endTime) {
        return Result.ok("片段识别成功", realtimeService.processChunk(audio, taskId, startTime, endTime));
    }

    @GetMapping("/status")
    public Result<RealtimeStatusVO> status() {
        return Result.ok(realtimeService.status());
    }
}
