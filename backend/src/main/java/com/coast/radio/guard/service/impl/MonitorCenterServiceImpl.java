package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.RiskLevel;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.entity.AsrResult;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.LlmAnalysisResult;
import com.coast.radio.guard.entity.RadioChannel;
import com.coast.radio.guard.entity.RiskEvent;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AsrResultMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.ChannelMapper;
import com.coast.radio.guard.mapper.LlmAnalysisResultMapper;
import com.coast.radio.guard.mapper.RiskEventMapper;
import com.coast.radio.guard.service.MonitorCenterService;
import com.coast.radio.guard.service.RealtimeService;
import com.coast.radio.guard.vo.monitor.MonitorCenterOverviewVO;
import com.coast.radio.guard.vo.monitor.MonitorRecentAlarmVO;
import com.coast.radio.guard.vo.monitor.MonitorRecentTaskVO;
import com.coast.radio.guard.vo.monitor.RealtimeStatusVO;
import com.coast.radio.guard.vo.structured.StructuredResultListVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorCenterServiceImpl implements MonitorCenterService {

    private final AudioTaskMapper audioTaskMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final ChannelMapper channelMapper;
    private final RiskEventMapper riskEventMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;
    private final AsrResultMapper asrResultMapper;
    private final RealtimeService realtimeService;

    public MonitorCenterServiceImpl(AudioTaskMapper audioTaskMapper,
                                    AlarmRecordMapper alarmRecordMapper,
                                    ChannelMapper channelMapper,
                                    RiskEventMapper riskEventMapper,
                                    LlmAnalysisResultMapper llmAnalysisResultMapper,
                                    AsrResultMapper asrResultMapper,
                                    RealtimeService realtimeService) {
        this.audioTaskMapper = audioTaskMapper;
        this.alarmRecordMapper = alarmRecordMapper;
        this.channelMapper = channelMapper;
        this.riskEventMapper = riskEventMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
        this.asrResultMapper = asrResultMapper;
        this.realtimeService = realtimeService;
    }

    @Override
    public MonitorCenterOverviewVO getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Long todayTaskCount = audioTaskMapper.selectCount(new LambdaQueryWrapper<AudioTask>()
            .ge(AudioTask::getCreateTime, todayStart));

        Long todayAlarmCount = alarmRecordMapper.selectCount(new LambdaQueryWrapper<AlarmRecord>()
            .ge(AlarmRecord::getCreateTime, todayStart));

        Long todayHighRiskEventCount = riskEventMapper.selectCount(new LambdaQueryWrapper<RiskEvent>()
            .eq(RiskEvent::getRiskLevel, RiskLevel.HIGH)
            .ge(RiskEvent::getCreateTime, todayStart));

        Long channelCount = channelMapper.selectCount(new LambdaQueryWrapper<RadioChannel>());

        List<MonitorRecentTaskVO> recentTasks = audioTaskMapper.selectList(new LambdaQueryWrapper<AudioTask>()
                .orderByDesc(AudioTask::getId)
                .last("LIMIT 8"))
            .stream()
            .map(task -> MonitorRecentTaskVO.builder()
                .taskId(task.getId())
                .channelId(task.getChannelId())
                .taskStatus(task.getTaskStatus())
                .riskLevel(task.getRiskLevel())
                .createTime(task.getCreateTime())
                .build())
            .toList();

        List<MonitorRecentAlarmVO> recentAlarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                .orderByDesc(AlarmRecord::getId)
                .last("LIMIT 8"))
            .stream()
            .map(alarm -> MonitorRecentAlarmVO.builder()
                .id(alarm.getId())
                .taskId(alarm.getTaskId())
                .analysisId(alarm.getAnalysisId())
                .channelId(alarm.getChannelId())
                .alarmLevel(alarm.getAlarmLevel())
                .alarmStatus(alarm.getAlarmStatus())
                .triggerSource(alarm.getTriggerSource())
                .triggerReason(alarm.getTriggerReason())
                .createTime(alarm.getCreateTime())
                .build())
            .toList();

        List<StructuredResultListVO> recentStructuredResults = llmAnalysisResultMapper.selectList(new LambdaQueryWrapper<LlmAnalysisResult>()
                .orderByDesc(LlmAnalysisResult::getId)
                .last("LIMIT 8"))
            .stream()
            .map(this::toStructuredPreview)
            .toList();

        return MonitorCenterOverviewVO.builder()
            .systemStatus("ONLINE")
            .realtimeStatus(getRealtimeStatus())
            .todayTaskCount(nvl(todayTaskCount))
            .todayAlarmCount(nvl(todayAlarmCount))
            .todayHighRiskEventCount(nvl(todayHighRiskEventCount))
            .channelCount(nvl(channelCount))
            .recentTasks(recentTasks)
            .recentAlarms(recentAlarms)
            .recentStructuredResults(recentStructuredResults)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    @Override
    public RealtimeStatusVO getRealtimeStatus() {
        return realtimeService.status();
    }

    private StructuredResultListVO toStructuredPreview(LlmAnalysisResult row) {
        AsrResult asr = asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, row.getTaskId())
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));

        return StructuredResultListVO.builder()
            .taskId(row.getTaskId())
            .transcriptText(crop(preferredTranscript(asr), 160))
            .rawTranscript(asr == null ? null : crop(asr.getRawTranscript(), 160))
            .correctedTranscript(asr == null ? null : crop(asr.getCorrectedTranscript(), 160))
            .correctionDiff(asr == null ? null : asr.getCorrectionDiff())
            .correctionFallback(asr != null && asr.getCorrectionFallback() != null && asr.getCorrectionFallback() == 1)
            .riskLevel(row.getRiskLevel())
            .eventType(row.getEventType())
            .eventSummary(crop(row.getEventSummary(), 160))
            .entitiesCount(null)
            .createTime(row.getCreateTime())
            .build();
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private String crop(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private String preferredTranscript(AsrResult asr) {
        if (asr == null) {
            return null;
        }
        if (asr.getCorrectedTranscript() != null && !asr.getCorrectedTranscript().isBlank()) {
            return asr.getCorrectedTranscript();
        }
        if (asr.getRawTranscript() != null && !asr.getRawTranscript().isBlank()) {
            return asr.getRawTranscript();
        }
        return asr.getTranscriptText();
    }
}
