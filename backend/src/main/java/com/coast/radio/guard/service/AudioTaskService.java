package com.coast.radio.guard.service;

import com.coast.radio.guard.dto.audio.AudioTaskCreateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskStatusUpdateDTO;
import com.coast.radio.guard.dto.audio.AudioTaskUpdateDTO;
import com.coast.radio.guard.vo.audio.AudioTaskVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.math.BigDecimal;

public interface AudioTaskService {

    List<AudioTaskVO> listTasks();

    AudioTaskVO getTask(Long id);

    Long createTask(AudioTaskCreateDTO dto);

    Long createRealtimeTask(Long channelId, String sourceSessionId, String mode);

    Long createTaskFromUpload(Long channelId, BigDecimal duration, MultipartFile file);

    void updateTask(Long id, AudioTaskUpdateDTO dto);

    void updateTaskStatus(Long id, AudioTaskStatusUpdateDTO dto);

    void deleteTask(Long id);
}
