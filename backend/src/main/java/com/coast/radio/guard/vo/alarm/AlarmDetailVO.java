package com.coast.radio.guard.vo.alarm;

import com.coast.radio.guard.vo.audio.AlarmRecordVO;
import com.coast.radio.guard.vo.audio.AsrResultVO;
import com.coast.radio.guard.vo.audio.AudioTaskVO;
import com.coast.radio.guard.vo.audio.LlmAnalysisResultVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlarmDetailVO {
    private AlarmRecordVO alarm;
    private AudioTaskVO task;
    private AsrResultVO asrResult;
    private LlmAnalysisResultVO llmAnalysisResult;
    private List<AlarmAuditLogVO> auditLogs;
}
