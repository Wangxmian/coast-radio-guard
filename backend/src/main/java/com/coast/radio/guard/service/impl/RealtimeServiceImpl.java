package com.coast.radio.guard.service.impl;

import com.coast.radio.guard.ai.client.RealtimeAsrClient;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.dto.ai.RealtimeTranscribeResponseDTO;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.service.AiOrchestrationService;
import com.coast.radio.guard.service.AudioTaskService;
import com.coast.radio.guard.service.RealtimeService;
import com.coast.radio.guard.vo.monitor.RealtimeChunkResultVO;
import com.coast.radio.guard.vo.monitor.RealtimeSegmentVO;
import com.coast.radio.guard.vo.monitor.RealtimeStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RealtimeServiceImpl implements RealtimeService {

    private static final int MAX_SEGMENTS = 30;

    private final RealtimeAsrClient realtimeAsrClient;
    private final AudioTaskService audioTaskService;
    private final AiOrchestrationService aiOrchestrationService;

    private final Object lock = new Object();
    private boolean active = false;
    private String mode = "manual";
    private String sessionId;
    private Long channelId;
    private Long currentTaskId;
    private String currentTranscript;
    private String latestRiskLevel;
    private String latestEventType;
    private Long latestAlarmId;
    private String latestAlarmLevel;
    private String latestAlarmStatus;
    private String latestAlarmReason;
    private LocalDateTime lastUpdatedAt;
    private Map<String, Object> providerInfo = Map.of();
    private String note = "实时监听未启动";
    private final List<RealtimeSegmentVO> recentSegments = new ArrayList<>();

    public RealtimeServiceImpl(RealtimeAsrClient realtimeAsrClient,
                               AudioTaskService audioTaskService,
                               AiOrchestrationService aiOrchestrationService) {
        this.realtimeAsrClient = realtimeAsrClient;
        this.audioTaskService = audioTaskService;
        this.aiOrchestrationService = aiOrchestrationService;
    }

    @Override
    public RealtimeStatusVO start(Long channelId, String mode) {
        if (channelId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "启动实时监听时必须提供 channelId");
        }
        String newSessionId = UUID.randomUUID().toString();
        Long taskId = audioTaskService.createRealtimeTask(channelId, newSessionId, mode);
        synchronized (lock) {
            this.active = true;
            this.mode = mode == null || mode.isBlank() ? "manual" : mode;
            this.sessionId = newSessionId;
            this.channelId = channelId;
            this.currentTaskId = taskId;
            this.currentTranscript = null;
            this.latestRiskLevel = null;
            this.latestEventType = null;
            this.latestAlarmId = null;
            this.latestAlarmLevel = null;
            this.latestAlarmStatus = null;
            this.latestAlarmReason = null;
            this.providerInfo = Map.of();
            this.note = "实时监听已启动，任务已入库，后续转录将自动进入分析与告警链路";
            this.lastUpdatedAt = LocalDateTime.now();
            this.recentSegments.clear();
            log.info("Realtime listening started, channelId={}, taskId={}, sessionId={}, mode={}",
                channelId, taskId, this.sessionId, this.mode);
            return buildStatus();
        }
    }

    @Override
    public RealtimeStatusVO stop() {
        synchronized (lock) {
            this.active = false;
            this.note = "实时监听已停止";
            this.lastUpdatedAt = LocalDateTime.now();
            log.info("Realtime listening stopped, channelId={}, taskId={}, mode={}", channelId, currentTaskId, mode);
            return buildStatus();
        }
    }

    @Override
    public RealtimeStatusVO status() {
        synchronized (lock) {
            return buildStatus();
        }
    }

    @Override
    public RealtimeChunkResultVO processChunk(MultipartFile file, Long taskId, Integer startTime, Integer endTime) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "音频片段不能为空");
        }

        synchronized (lock) {
            if (!active) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "实时监听未启动，请先调用 /api/realtime/start");
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "读取音频片段失败: " + ex.getMessage());
        }

        Long effectiveTaskId = taskId;
        Long effectiveChannelId;
        String effectiveSessionId;
        synchronized (lock) {
            effectiveTaskId = effectiveTaskId == null ? currentTaskId : effectiveTaskId;
            effectiveChannelId = channelId;
            effectiveSessionId = sessionId;
        }
        if (effectiveTaskId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "实时监听任务未初始化，请重新启动监听");
        }

        RealtimeTranscribeResponseDTO resp = realtimeAsrClient.transcribeChunk(
            bytes,
            file.getOriginalFilename(),
            effectiveTaskId,
            startTime,
            endTime
        );

        RealtimeChunkResultVO result = aiOrchestrationService.ingestRealtimeTranscript(
            effectiveChannelId,
            effectiveTaskId,
            effectiveSessionId,
            resp.getTranscriptText(),
            null,
            resp.getLanguage(),
            resp.getStartTime(),
            resp.getEndTime(),
            resp.getProviderInfo(),
            resp.getStageInfo(),
            resp.getFallbackInfo()
        );

        String segmentId = (resp.getSegmentId() == null || resp.getSegmentId().isBlank())
            ? result.getSegmentId()
            : resp.getSegmentId();

        RealtimeSegmentVO segment = RealtimeSegmentVO.builder()
            .segmentId(segmentId)
            .taskId(result.getTaskId())
            .transcriptText(resp.getTranscriptText())
            .startTime(resp.getStartTime())
            .endTime(resp.getEndTime())
            .isFinal(resp.getIsFinal())
            .analysisId(result.getAnalysisId())
            .alarmId(result.getAlarmId())
            .riskLevel(result.getRiskLevel())
            .eventType(result.getEventType())
            .providerInfo(resp.getProviderInfo() == null ? Map.of() : resp.getProviderInfo())
            .timestamp(result.getTimestamp())
            .build();

        synchronized (lock) {
            this.currentTaskId = result.getTaskId();
            this.currentTranscript = resp.getTranscriptText();
            this.providerInfo = result.getProviderInfo() == null ? Map.of() : result.getProviderInfo();
            this.latestRiskLevel = result.getRiskLevel();
            this.latestEventType = result.getEventType();
            this.latestAlarmId = result.getAlarmId();
            this.latestAlarmLevel = result.getAlarmLevel();
            this.latestAlarmStatus = result.getAlarmStatus();
            this.latestAlarmReason = result.getTriggerReason();
            this.note = result.getAlarmId() == null ? "实时识别与分析已更新" : "检测到高风险告警，请立即关注";
            this.lastUpdatedAt = result.getTimestamp();
            this.recentSegments.add(0, segment);
            while (this.recentSegments.size() > MAX_SEGMENTS) {
                this.recentSegments.remove(this.recentSegments.size() - 1);
            }
        }

        log.info("Realtime chunk processed, taskId={}, segmentId={}, transcriptLen={}, alarmId={}",
            effectiveTaskId, segmentId, resp.getTranscriptText() == null ? 0 : resp.getTranscriptText().length(), result.getAlarmId());

        return RealtimeChunkResultVO.builder()
            .segmentId(segmentId)
            .taskId(result.getTaskId())
            .transcriptText(result.getTranscriptText())
            .startTime(result.getStartTime())
            .endTime(result.getEndTime())
            .isFinal(result.getIsFinal())
            .analysisId(result.getAnalysisId())
            .alarmId(result.getAlarmId())
            .alarmLevel(result.getAlarmLevel())
            .alarmStatus(result.getAlarmStatus())
            .riskLevel(result.getRiskLevel())
            .eventType(result.getEventType())
            .eventSummary(result.getEventSummary())
            .triggerSource(result.getTriggerSource())
            .triggerReason(result.getTriggerReason())
            .providerInfo(result.getProviderInfo())
            .stageInfo(result.getStageInfo())
            .fallbackInfo(result.getFallbackInfo())
            .timestamp(result.getTimestamp())
            .build();
    }

    @Override
    public Long currentTaskId() {
        synchronized (lock) {
            return currentTaskId;
        }
    }

    private RealtimeStatusVO buildStatus() {
        return RealtimeStatusVO.builder()
            .listeningStatus(active ? "LISTENING" : "IDLE")
            .streamStatus(active ? "READY" : "NOT_CONNECTED")
            .sessionId(sessionId)
            .currentChannel(channelId)
            .currentTaskId(currentTaskId)
            .currentTranscript(currentTranscript)
            .latestRiskLevel(latestRiskLevel)
            .latestEventType(latestEventType)
            .latestAlarmId(latestAlarmId)
            .latestAlarmLevel(latestAlarmLevel)
            .latestAlarmStatus(latestAlarmStatus)
            .latestAlarmReason(latestAlarmReason)
            .recentSegments(List.copyOf(recentSegments))
            .providerInfo(providerInfo)
            .message(note)
            .note(note)
            .capabilityNote("最小可用版本：前端上传短片段 + 后端轮询状态")
            .lastUpdatedAt(lastUpdatedAt == null ? LocalDateTime.now() : lastUpdatedAt)
            .build();
    }
}
