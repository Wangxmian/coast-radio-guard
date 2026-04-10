package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.audio.AudioTaskCreateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskStatusUpdateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskUpdateDTO;
import com.coast.radio.guard.service.AiOrchestrationService;
import com.coast.radio.guard.service.AudioTaskService;
import com.coast.radio.guard.vo.audio.AnalysisDetailVO;
import com.coast.radio.guard.vo.audio.AudioTaskVO;
import com.coast.radio.guard.vo.audio.ExecuteSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audio-tasks")
public class AudioTaskController {

    private final AudioTaskService audioTaskService;
    private final AiOrchestrationService aiOrchestrationService;

    public AudioTaskController(AudioTaskService audioTaskService,
                               AiOrchestrationService aiOrchestrationService) {
        this.audioTaskService = audioTaskService;
        this.aiOrchestrationService = aiOrchestrationService;
    }

    @GetMapping
    public Result<List<AudioTaskVO>> listTasks() {
        return Result.ok(audioTaskService.listTasks());
    }

    @GetMapping("/{id}")
    public Result<AudioTaskVO> getTask(@PathVariable Long id) {
        return Result.ok(audioTaskService.getTask(id));
    }

    @PostMapping
    public Result<Map<String, Long>> createTask(@Valid @RequestBody AudioTaskCreateDTO dto) {
        Long id = audioTaskService.createTask(dto);
        return Result.ok("创建成功", Map.of("id", id));
    }

    @PostMapping("/upload")
    public Result<Map<String, Long>> uploadTask(@RequestParam("channelId") Long channelId,
                                                @RequestParam(value = "duration", required = false) BigDecimal duration,
                                                @RequestParam("audio") MultipartFile audio) {
        Long id = audioTaskService.createTaskFromUpload(channelId, duration, audio);
        return Result.ok("上传并创建任务成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @Valid @RequestBody AudioTaskUpdateDTO dto) {
        audioTaskService.updateTask(id, dto);
        return Result.ok("更新成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateTaskStatus(@PathVariable Long id, @Valid @RequestBody AudioTaskStatusUpdateDTO dto) {
        audioTaskService.updateTaskStatus(id, dto);
        return Result.ok("状态更新成功", null);
    }

    @PostMapping("/{id}/execute")
    public Result<ExecuteSummaryVO> executeTask(@PathVariable Long id) {
        return Result.ok("任务执行完成", aiOrchestrationService.executeTask(id));
    }

    @GetMapping("/{id}/analysis")
    public Result<AnalysisDetailVO> getAnalysis(@PathVariable Long id) {
        return Result.ok(aiOrchestrationService.getAnalysisDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        audioTaskService.deleteTask(id);
        return Result.ok("删除成功", null);
    }
}
