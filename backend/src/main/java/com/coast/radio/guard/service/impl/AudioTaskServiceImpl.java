package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.common.constants.TaskStatus;
import com.coast.radio.guard.dto.audio.AudioTaskCreateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskStatusUpdateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskUpdateDTO;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.RadioChannel;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.ChannelMapper;
import com.coast.radio.guard.service.AudioTaskService;
import com.coast.radio.guard.vo.audio.AudioTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class AudioTaskServiceImpl implements AudioTaskService {

    private static final String TASK_TYPE_OFFLINE = "OFFLINE";
    private static final Path UPLOAD_DIR = Path.of(System.getProperty("user.dir"), "data", "uploads", "audio");

    private final AudioTaskMapper audioTaskMapper;
    private final ChannelMapper channelMapper;

    public AudioTaskServiceImpl(AudioTaskMapper audioTaskMapper, ChannelMapper channelMapper) {
        this.audioTaskMapper = audioTaskMapper;
        this.channelMapper = channelMapper;
    }

    @Override
    public List<AudioTaskVO> listTasks() {
        return audioTaskMapper.selectList(new LambdaQueryWrapper<AudioTask>()
                .orderByDesc(AudioTask::getCreateTime)
                .orderByDesc(AudioTask::getId))
            .stream().map(this::toVO).toList();
    }

    @Override
    public AudioTaskVO getTask(Long id) {
        return toVO(requireById(id));
    }

    @Override
    public Long createTask(AudioTaskCreateDTO dto) {
        requireChannel(dto.getChannelId());
        LocalDateTime now = LocalDateTime.now();

        AudioTask task = new AudioTask();
        task.setChannelId(dto.getChannelId());
        task.setOriginalFilePath(dto.getOriginalFilePath());
        task.setEnhancedFilePath(dto.getEnhancedFilePath());
        task.setTaskType(TASK_TYPE_OFFLINE);
        task.setTaskStatus(TaskStatus.WAITING);
        task.setSeStatus(TaskStatus.WAITING);
        task.setAsrStatus(TaskStatus.WAITING);
        task.setLlmStatus(TaskStatus.WAITING);
        task.setDuration(dto.getDuration());
        task.setErrorMsg(dto.getErrorMsg());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        audioTaskMapper.insert(task);
        log.info("Created audio task {} for channel {}", task.getId(), task.getChannelId());
        return task.getId();
    }

    @Override
    public Long createRealtimeTask(Long channelId, String sourceSessionId, String mode) {
        requireChannel(channelId);
        LocalDateTime now = LocalDateTime.now();

        AudioTask task = new AudioTask();
        task.setChannelId(channelId);
        task.setOriginalFilePath(buildRealtimePath(channelId, sourceSessionId, mode));
        task.setEnhancedFilePath(buildRealtimePath(channelId, sourceSessionId, mode));
        task.setTaskType("REALTIME");
        task.setSourceSessionId(sourceSessionId);
        task.setTaskStatus(TaskStatus.PROCESSING);
        task.setSeStatus(TaskStatus.SUCCESS);
        task.setAsrStatus(TaskStatus.PROCESSING);
        task.setLlmStatus(TaskStatus.WAITING);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setExecuteTime(now);
        audioTaskMapper.insert(task);
        log.info("Created realtime audio task {} for channel {}, session={}", task.getId(), channelId, sourceSessionId);
        return task.getId();
    }

    @Override
    public Long createTaskFromUpload(Long channelId, BigDecimal duration, MultipartFile file) {
        requireChannel(channelId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传音频不能为空");
        }
        String storedPath = storeUpload(file);

        AudioTaskCreateDTO dto = new AudioTaskCreateDTO();
        dto.setChannelId(channelId);
        dto.setOriginalFilePath(storedPath);
        dto.setDuration(duration);
        return createTask(dto);
    }

    @Override
    public void updateTask(Long id, AudioTaskUpdateDTO dto) {
        requireChannel(dto.getChannelId());
        AudioTask task = requireById(id);
        task.setChannelId(dto.getChannelId());
        if (dto.getOriginalFilePath() != null) {
            task.setOriginalFilePath(dto.getOriginalFilePath());
        }
        task.setEnhancedFilePath(dto.getEnhancedFilePath());
        task.setDuration(dto.getDuration());
        task.setErrorMsg(dto.getErrorMsg());
        task.setUpdateTime(LocalDateTime.now());
        audioTaskMapper.updateById(task);
    }

    @Override
    public void updateTaskStatus(Long id, AudioTaskStatusUpdateDTO dto) {
        if (!TaskStatus.isValid(dto.getTaskStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法任务状态: " + dto.getTaskStatus());
        }
        AudioTask task = requireById(id);
        task.setTaskStatus(dto.getTaskStatus());
        task.setErrorMsg(dto.getErrorMsg());
        task.setUpdateTime(LocalDateTime.now());
        if (TaskStatus.SUCCESS.equals(dto.getTaskStatus()) || TaskStatus.FAILED.equals(dto.getTaskStatus())) {
            task.setFinishTime(LocalDateTime.now());
        }
        audioTaskMapper.updateById(task);
    }

    @Override
    public void deleteTask(Long id) {
        requireById(id);
        audioTaskMapper.deleteById(id);
        log.info("Deleted audio task {}", id);
    }

    private AudioTask requireById(Long id) {
        AudioTask task = audioTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private void requireChannel(Long channelId) {
        RadioChannel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关联频道不存在");
        }
    }

    private AudioTaskVO toVO(AudioTask task) {
        return AudioTaskVO.builder()
            .id(task.getId())
            .channelId(task.getChannelId())
            .originalFilePath(task.getOriginalFilePath())
            .enhancedFilePath(task.getEnhancedFilePath())
            .taskType(task.getTaskType())
            .sourceSessionId(task.getSourceSessionId())
            .taskStatus(task.getTaskStatus())
            .seStatus(task.getSeStatus())
            .asrStatus(task.getAsrStatus())
            .llmStatus(task.getLlmStatus())
            .transcriptText(task.getTranscriptText())
            .riskLevel(task.getRiskLevel())
            .duration(task.getDuration())
            .errorMsg(task.getErrorMsg())
            .lastErrorMsg(task.getLastErrorMsg())
            .executeTime(task.getExecuteTime())
            .lastTranscriptTime(task.getLastTranscriptTime())
            .createTime(task.getCreateTime())
            .finishTime(task.getFinishTime())
            .updateTime(task.getUpdateTime())
            .build();
    }

    private String buildRealtimePath(Long channelId, String sourceSessionId, String mode) {
        String safeMode = mode == null || mode.isBlank() ? "manual" : mode.trim().toLowerCase(Locale.ROOT);
        return "realtime://channel/" + channelId + "/session/" + sourceSessionId + "?mode=" + safeMode;
    }

    private String storeUpload(MultipartFile file) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String originalName = file.getOriginalFilename() == null ? "audio.wav" : file.getOriginalFilename();
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot);
            }
            String safeName = "task_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = UPLOAD_DIR.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "保存上传音频失败: " + ex.getMessage());
        }
    }
}
