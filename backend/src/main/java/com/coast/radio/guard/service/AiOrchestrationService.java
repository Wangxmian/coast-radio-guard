package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.audio.AnalysisDetailVO;
import com.coast.radio.guard.vo.audio.ExecuteSummaryVO;
import com.coast.radio.guard.vo.monitor.RealtimeChunkResultVO;

import java.util.Map;

public interface AiOrchestrationService {

    ExecuteSummaryVO executeTask(Long taskId);

    AnalysisDetailVO getAnalysisDetail(Long taskId);

    RealtimeChunkResultVO ingestRealtimeTranscript(Long channelId,
                                                   Long taskId,
                                                   String sourceSessionId,
                                                   String transcriptText,
                                                   Double confidence,
                                                   String language,
                                                   Integer startTime,
                                                   Integer endTime,
                                                   Map<String, Object> providerInfo,
                                                   Map<String, Object> stageInfo,
                                                   Map<String, Object> fallbackInfo);
}
