package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.entity.AsrResult;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.LlmAnalysisResult;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AsrResultMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.LlmAnalysisResultMapper;
import com.coast.radio.guard.service.HistoryRecordService;
import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.history.HistoryRecordVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class HistoryRecordServiceImpl implements HistoryRecordService {

    private final AudioTaskMapper audioTaskMapper;
    private final AsrResultMapper asrResultMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;
    private final AlarmRecordMapper alarmRecordMapper;

    public HistoryRecordServiceImpl(AudioTaskMapper audioTaskMapper,
                                    AsrResultMapper asrResultMapper,
                                    LlmAnalysisResultMapper llmAnalysisResultMapper,
                                    AlarmRecordMapper alarmRecordMapper) {
        this.audioTaskMapper = audioTaskMapper;
        this.asrResultMapper = asrResultMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
        this.alarmRecordMapper = alarmRecordMapper;
    }

    @Override
    public PageResultVO<HistoryRecordVO> queryHistoryRecords(Long page,
                                                             Long pageSize,
                                                             Long taskId,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime,
                                                             String keyword,
                                                             String taskStatus,
                                                             String riskLevel,
                                                             Long channelId,
                                                             String alarmLevel) {
        long safePage = page == null || page < 1 ? 1 : page;
        long safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        String normalizedTaskStatus = taskStatus == null ? null : taskStatus.trim().toUpperCase(Locale.ROOT);
        String normalizedRiskLevel = riskLevel == null ? null : riskLevel.trim().toUpperCase(Locale.ROOT);

        LambdaQueryWrapper<AudioTask> qw = new LambdaQueryWrapper<>();
        qw.eq(taskId != null, AudioTask::getId, taskId)
            .eq(normalizedTaskStatus != null && !normalizedTaskStatus.isBlank(), AudioTask::getTaskStatus, normalizedTaskStatus)
            .eq(normalizedRiskLevel != null && !normalizedRiskLevel.isBlank(), AudioTask::getRiskLevel, normalizedRiskLevel)
            .eq(channelId != null, AudioTask::getChannelId, channelId)
            .ge(startTime != null, AudioTask::getCreateTime, startTime)
            .le(endTime != null, AudioTask::getCreateTime, endTime)
            .orderByDesc(AudioTask::getId);

        List<AudioTask> tasks = audioTaskMapper.selectList(qw);
        List<HistoryRecordVO> all = new ArrayList<>();
        for (AudioTask task : tasks) {
            AsrResult asr = latestAsr(task.getId());
            LlmAnalysisResult llm = latestLlm(task.getId());
            AlarmRecord alarm = latestAlarm(task.getId());
            HistoryRecordVO row = HistoryRecordVO.builder()
                .taskId(task.getId())
                .channelId(task.getChannelId())
                .analysisId(llm == null ? null : llm.getId())
                .alarmId(alarm == null ? null : alarm.getId())
                .taskType(task.getTaskType())
                .taskStatus(task.getTaskStatus())
                .createTime(task.getCreateTime())
                .transcriptText(asr == null ? task.getTranscriptText() : preferredTranscript(asr, task))
                .rawTranscript(asr == null ? task.getTranscriptText() : asr.getRawTranscript())
                .correctedTranscript(asr == null ? task.getTranscriptText() : asr.getCorrectedTranscript())
                .correctionDiff(asr == null ? null : asr.getCorrectionDiff())
                .correctionFallback(asr != null && asr.getCorrectionFallback() != null && asr.getCorrectionFallback() == 1)
                .riskLevel(llm == null ? task.getRiskLevel() : llm.getRiskLevel())
                .eventType(llm == null ? null : llm.getEventType())
                .eventSummary(llm == null ? null : llm.getEventSummary())
                .alarmLevel(alarm == null ? null : alarm.getAlarmLevel())
                .alarmStatus(alarm == null ? null : alarm.getAlarmStatus())
                .triggerReason(alarm == null ? null : crop(alarm.getTriggerReason(), 180))
                .hasAlarm(alarm != null)
                .build();

            if (!matchKeyword(row, keyword)) {
                continue;
            }
            if (alarmLevel != null && !alarmLevel.isBlank()) {
                if (row.getAlarmLevel() == null || !alarmLevel.equalsIgnoreCase(row.getAlarmLevel())) {
                    continue;
                }
            }
            all.add(row);
        }

        int from = (int) ((safePage - 1) * safePageSize);
        if (from >= all.size()) {
            return PageResultVO.<HistoryRecordVO>builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(all.size())
                .records(List.of())
                .build();
        }

        int to = (int) Math.min(from + safePageSize, all.size());
        return PageResultVO.<HistoryRecordVO>builder()
            .page(safePage)
            .pageSize(safePageSize)
            .total(all.size())
            .records(all.subList(from, to))
            .build();
    }

    @Override
    public ResponseEntity<byte[]> exportHistoryRecords(LocalDateTime startTime,
                                                       LocalDateTime endTime,
                                                       Long taskId,
                                                       String keyword,
                                                       String taskStatus,
                                                       String riskLevel,
                                                       String alarmLevel) {
        PageResultVO<HistoryRecordVO> page = queryHistoryRecords(
            1L, 10000L, taskId, startTime, endTime, keyword, taskStatus, riskLevel, null, alarmLevel);

        StringBuilder sb = new StringBuilder();
        sb.append("taskId,channelId,taskStatus,createTime,rawTranscript,correctedTranscript,riskLevel,eventType,alarmLevel,alarmStatus,triggerReason\n");
        for (HistoryRecordVO row : page.getRecords()) {
            sb.append(csv(row.getTaskId())).append(',')
                .append(csv(row.getChannelId())).append(',')
                .append(csv(row.getTaskStatus())).append(',')
                .append(csv(row.getCreateTime())).append(',')
                .append(csv(row.getRawTranscript())).append(',')
                .append(csv(row.getCorrectedTranscript())).append(',')
                .append(csv(row.getRiskLevel())).append(',')
                .append(csv(row.getEventType())).append(',')
                .append(csv(row.getAlarmLevel())).append(',')
                .append(csv(row.getAlarmStatus())).append(',')
                .append(csv(row.getTriggerReason()))
                .append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "history_records_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private AsrResult latestAsr(Long taskId) {
        return asrResultMapper.selectOne(new LambdaQueryWrapper<AsrResult>()
            .eq(AsrResult::getTaskId, taskId)
            .orderByDesc(AsrResult::getId)
            .last("LIMIT 1"));
    }

    private LlmAnalysisResult latestLlm(Long taskId) {
        return llmAnalysisResultMapper.selectOne(new LambdaQueryWrapper<LlmAnalysisResult>()
            .eq(LlmAnalysisResult::getTaskId, taskId)
            .orderByDesc(LlmAnalysisResult::getId)
            .last("LIMIT 1"));
    }

    private AlarmRecord latestAlarm(Long taskId) {
        return alarmRecordMapper.selectOne(new LambdaQueryWrapper<AlarmRecord>()
            .eq(AlarmRecord::getTaskId, taskId)
            .orderByDesc(AlarmRecord::getId)
            .last("LIMIT 1"));
    }

    private boolean matchKeyword(HistoryRecordVO row, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String k = keyword.toLowerCase(Locale.ROOT);
        return contains(row.getTaskId(), k)
            || contains(row.getTranscriptText(), k)
            || contains(row.getRawTranscript(), k)
            || contains(row.getCorrectedTranscript(), k)
            || contains(row.getEventType(), k)
            || contains(row.getTriggerReason(), k)
            || contains(row.getTaskStatus(), k);
    }

    private String preferredTranscript(AsrResult asr, AudioTask task) {
        if (asr != null && asr.getCorrectedTranscript() != null && !asr.getCorrectedTranscript().isBlank()) {
            return asr.getCorrectedTranscript();
        }
        if (asr != null && asr.getRawTranscript() != null && !asr.getRawTranscript().isBlank()) {
            return asr.getRawTranscript();
        }
        return task == null ? null : task.getTranscriptText();
    }

    private boolean contains(Object value, String keyword) {
        if (value == null) {
            return false;
        }
        return String.valueOf(value).toLowerCase(Locale.ROOT).contains(keyword);
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

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
