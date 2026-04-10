package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisDetailVO {
    private AudioTaskVO task;
    private AsrResultVO asrResult;
    private LlmAnalysisResultVO llmAnalysisResult;
    private List<EntityResultVO> entityResults;
    private RiskEventVO riskEvent;
    private List<AlarmRecordVO> alarmRecords;
    private String analysisTranscript;
    private Boolean analysisUsesCorrectedTranscript;
    private Map<String, Object> providerInfo;
    private Map<String, Object> stageInfo;
    private Map<String, Object> fallbackInfo;
}
