package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.monitor.RealtimeChunkResultVO;
import com.coast.radio.guard.vo.monitor.RealtimeStatusVO;
import org.springframework.web.multipart.MultipartFile;

public interface RealtimeService {

    RealtimeStatusVO start(Long channelId, String mode);

    RealtimeStatusVO stop();

    RealtimeStatusVO status();

    RealtimeChunkResultVO processChunk(MultipartFile file, Long taskId, Integer startTime, Integer endTime);

    Long currentTaskId();
}
