package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.AlarmLevel;
import com.coast.radio.guard.common.constants.AlarmStatus;
import com.coast.radio.guard.common.constants.TaskStatus;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.entity.LlmAnalysisResult;
import com.coast.radio.guard.entity.RadioChannel;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.mapper.ChannelMapper;
import com.coast.radio.guard.mapper.LlmAnalysisResultMapper;
import com.coast.radio.guard.service.AgentQueryService;
import com.coast.radio.guard.service.model.AgentIntentType;
import com.coast.radio.guard.service.model.AgentStructuredResult;
import com.coast.radio.guard.service.model.AlarmListDTO;
import com.coast.radio.guard.service.model.AlarmStatisticsDTO;
import com.coast.radio.guard.service.model.AlarmSummaryDTO;
import com.coast.radio.guard.service.model.AlarmTrendPointDTO;
import com.coast.radio.guard.service.model.AlarmTrendSummaryDTO;
import com.coast.radio.guard.service.model.LatestAlarmItemDTO;
import com.coast.radio.guard.service.model.LatestTaskItemDTO;
import com.coast.radio.guard.service.model.TaskStatusSummaryDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgentQueryServiceImpl implements AgentQueryService {

    private static final int DEFAULT_LIST_LIMIT = 5;

    private final AlarmRecordMapper alarmRecordMapper;
    private final AudioTaskMapper audioTaskMapper;
    private final ChannelMapper channelMapper;
    private final LlmAnalysisResultMapper llmAnalysisResultMapper;

    public AgentQueryServiceImpl(AlarmRecordMapper alarmRecordMapper,
                                 AudioTaskMapper audioTaskMapper,
                                 ChannelMapper channelMapper,
                                 LlmAnalysisResultMapper llmAnalysisResultMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.audioTaskMapper = audioTaskMapper;
        this.channelMapper = channelMapper;
        this.llmAnalysisResultMapper = llmAnalysisResultMapper;
    }

    @Override
    public AgentStructuredResult query(String question, AgentIntentType intent) {
        return switch (intent) {
            case ALARM_STATS -> buildSupported(intent, queryTodayAlarmStats());
            case LATEST_HIGH_RISK_EVENTS -> buildSupported(intent, queryLatestHighRiskEvents(DEFAULT_LIST_LIMIT, "最近高风险事件"));
            case UNHANDLED_ALARMS -> buildSupported(intent, queryUnhandledAlarms(DEFAULT_LIST_LIMIT));
            case RECENT_ALARMS -> buildSupported(intent, queryRecentAlarms(DEFAULT_LIST_LIMIT));
            case TASK_STATUS -> buildSupported(intent, queryTodayTaskStatus());
            case DAILY_SUMMARY -> buildSupported(intent, queryDailySummary());
            case ALARM_TREND -> buildSupported(intent, queryAlarmTrend(7));
            case UNKNOWN -> AgentStructuredResult.builder()
                .intent(intent)
                .supported(false)
                .hasData(false)
                .build();
        };
    }

    private AgentStructuredResult buildSupported(AgentIntentType intent, Object structuredData) {
        return AgentStructuredResult.builder()
            .intent(intent)
            .supported(true)
            .hasData(hasData(structuredData))
            .structuredData(structuredData)
            .build();
    }

    private AlarmStatisticsDTO queryTodayAlarmStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long total = countAlarms(start, end, null, null);
        long high = countAlarms(start, end, AlarmLevel.HIGH, null);
        return AlarmStatisticsDTO.builder()
            .startTime(start)
            .endTime(end)
            .totalAlarmCount(total)
            .highRiskAlarmCount(high)
            .highRiskRatio(ratio(high, total))
            .latestHighRiskAlarm(fetchAlarmItems(
                new LambdaQueryWrapper<AlarmRecord>()
                    .eq(AlarmRecord::getAlarmLevel, AlarmLevel.HIGH)
                    .ge(AlarmRecord::getCreateTime, start)
                    .lt(AlarmRecord::getCreateTime, end)
                    .orderByDesc(AlarmRecord::getCreateTime)
                    .last("LIMIT 1")
            ).stream().findFirst().orElse(null))
            .build();
    }

    private AlarmListDTO queryLatestHighRiskEvents(int limit, String scope) {
        List<LatestAlarmItemDTO> items = fetchAlarmItems(
            new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getAlarmLevel, AlarmLevel.HIGH)
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("LIMIT " + limit)
        );
        return AlarmListDTO.builder()
            .scope(scope)
            .totalCount(items.size())
            .items(items)
            .build();
    }

    private AlarmListDTO queryUnhandledAlarms(int limit) {
        List<LatestAlarmItemDTO> items = fetchAlarmItems(
            new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getAlarmStatus, AlarmStatus.UNHANDLED)
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("LIMIT " + limit)
        );
        long total = countAlarms(null, null, null, AlarmStatus.UNHANDLED);
        return AlarmListDTO.builder()
            .scope("当前未处理告警")
            .totalCount(total)
            .items(items)
            .build();
    }

    private AlarmListDTO queryRecentAlarms(int limit) {
        List<LatestAlarmItemDTO> items = fetchAlarmItems(
            new LambdaQueryWrapper<AlarmRecord>()
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("LIMIT " + limit)
        );
        return AlarmListDTO.builder()
            .scope("最近告警")
            .totalCount(items.size())
            .items(items)
            .build();
    }

    private TaskStatusSummaryDTO queryTodayTaskStatus() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<AudioTask> todayTasks = audioTaskMapper.selectList(new LambdaQueryWrapper<AudioTask>()
            .ge(AudioTask::getCreateTime, start)
            .lt(AudioTask::getCreateTime, end)
            .orderByDesc(AudioTask::getCreateTime));
        List<LatestTaskItemDTO> failedTasks = toTaskItems(
            todayTasks.stream()
                .filter(task -> TaskStatus.FAILED.equalsIgnoreCase(task.getTaskStatus()))
                .limit(DEFAULT_LIST_LIMIT)
                .toList()
        );
        long successCount = todayTasks.stream().filter(task -> TaskStatus.SUCCESS.equalsIgnoreCase(task.getTaskStatus())).count();
        long failedCount = todayTasks.stream().filter(task -> TaskStatus.FAILED.equalsIgnoreCase(task.getTaskStatus())).count();
        long processingCount = todayTasks.stream().filter(task -> TaskStatus.PROCESSING.equalsIgnoreCase(task.getTaskStatus())).count();
        long waitingCount = todayTasks.stream().filter(task -> TaskStatus.WAITING.equalsIgnoreCase(task.getTaskStatus())).count();
        long total = todayTasks.size();
        return TaskStatusSummaryDTO.builder()
            .date(today)
            .totalTaskCount(total)
            .successTaskCount(successCount)
            .failedTaskCount(failedCount)
            .processingTaskCount(processingCount)
            .waitingTaskCount(waitingCount)
            .completionRate(ratio(successCount, total))
            .failedTasks(failedTasks)
            .build();
    }

    private AlarmSummaryDTO queryDailySummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long totalAlarms = countAlarms(start, end, null, null);
        long highRiskAlarms = countAlarms(start, end, AlarmLevel.HIGH, null);
        long unhandledAlarms = countAlarms(start, end, null, AlarmStatus.UNHANDLED);
        TaskStatusSummaryDTO taskSummary = queryTodayTaskStatus();
        List<LatestAlarmItemDTO> unhandledItems = fetchAlarmItems(
            new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getAlarmStatus, AlarmStatus.UNHANDLED)
                .ge(AlarmRecord::getCreateTime, start)
                .lt(AlarmRecord::getCreateTime, end)
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("LIMIT 3")
        );
        LatestAlarmItemDTO latestHighRiskAlarm = fetchAlarmItems(
            new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getAlarmLevel, AlarmLevel.HIGH)
                .ge(AlarmRecord::getCreateTime, start)
                .lt(AlarmRecord::getCreateTime, end)
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
        return AlarmSummaryDTO.builder()
            .date(today)
            .totalAlarmCount(totalAlarms)
            .highRiskAlarmCount(highRiskAlarms)
            .unhandledAlarmCount(unhandledAlarms)
            .totalTaskCount(taskSummary.getTotalTaskCount())
            .successTaskCount(taskSummary.getSuccessTaskCount())
            .failedTaskCount(taskSummary.getFailedTaskCount())
            .latestHighRiskAlarm(latestHighRiskAlarm)
            .recentUnhandledAlarms(unhandledItems)
            .build();
    }

    private AlarmTrendSummaryDTO queryAlarmTrend(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1L).atStartOfDay();
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
            .ge(AlarmRecord::getCreateTime, start)
            .lt(AlarmRecord::getCreateTime, end));

        Map<LocalDate, List<AlarmRecord>> grouped = alarms.stream()
            .filter(alarm -> alarm.getCreateTime() != null)
            .collect(Collectors.groupingBy(alarm -> alarm.getCreateTime().toLocalDate()));

        List<AlarmTrendPointDTO> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            List<AlarmRecord> rows = grouped.getOrDefault(date, List.of());
            long highRiskCount = rows.stream()
                .filter(alarm -> AlarmLevel.HIGH.equalsIgnoreCase(alarm.getAlarmLevel()))
                .count();
            points.add(AlarmTrendPointDTO.builder()
                .date(date)
                .alarmCount(rows.size())
                .highRiskCount(highRiskCount)
                .build());
        }

        AlarmTrendPointDTO peak = points.stream()
            .max(Comparator.comparingLong(AlarmTrendPointDTO::getAlarmCount))
            .orElse(null);

        return AlarmTrendSummaryDTO.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalAlarmCount(points.stream().mapToLong(AlarmTrendPointDTO::getAlarmCount).sum())
            .totalHighRiskCount(points.stream().mapToLong(AlarmTrendPointDTO::getHighRiskCount).sum())
            .peakAlarmDate(peak == null ? null : peak.getDate())
            .peakAlarmCount(peak == null ? 0L : peak.getAlarmCount())
            .points(points)
            .build();
    }

    private long countAlarms(LocalDateTime start, LocalDateTime end, String alarmLevel, String alarmStatus) {
        return alarmRecordMapper.selectCount(new LambdaQueryWrapper<AlarmRecord>()
            .eq(alarmLevel != null, AlarmRecord::getAlarmLevel, alarmLevel)
            .eq(alarmStatus != null, AlarmRecord::getAlarmStatus, alarmStatus)
            .ge(start != null, AlarmRecord::getCreateTime, start)
            .lt(end != null, AlarmRecord::getCreateTime, end));
    }

    private List<LatestAlarmItemDTO> fetchAlarmItems(LambdaQueryWrapper<AlarmRecord> wrapper) {
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(wrapper);
        if (alarms.isEmpty()) {
            return List.of();
        }
        Map<Long, RadioChannel> channelMap = channelMapper.selectBatchIds(
                alarms.stream()
                    .map(AlarmRecord::getChannelId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()
            ).stream()
            .collect(Collectors.toMap(RadioChannel::getId, Function.identity(), (a, b) -> a));

        Map<Long, LlmAnalysisResult> llmById = llmAnalysisResultMapper.selectBatchIds(
                alarms.stream()
                    .map(AlarmRecord::getAnalysisId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()
            ).stream()
            .collect(Collectors.toMap(LlmAnalysisResult::getId, Function.identity(), this::chooseLatest));

        List<Long> taskIds = alarms.stream()
            .map(AlarmRecord::getTaskId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, LlmAnalysisResult> latestLlmByTaskId = taskIds.isEmpty()
            ? Map.of()
            : llmAnalysisResultMapper.selectList(new LambdaQueryWrapper<LlmAnalysisResult>()
                .in(LlmAnalysisResult::getTaskId, taskIds)
                .orderByDesc(LlmAnalysisResult::getCreateTime))
                .stream()
                .collect(Collectors.toMap(LlmAnalysisResult::getTaskId, Function.identity(), this::chooseLatest));

        return alarms.stream()
            .map(alarm -> toAlarmItem(alarm, channelMap, llmById, latestLlmByTaskId))
            .toList();
    }

    private LatestAlarmItemDTO toAlarmItem(AlarmRecord alarm,
                                           Map<Long, RadioChannel> channelMap,
                                           Map<Long, LlmAnalysisResult> llmById,
                                           Map<Long, LlmAnalysisResult> latestLlmByTaskId) {
        RadioChannel channel = alarm.getChannelId() == null ? null : channelMap.get(alarm.getChannelId());
        LlmAnalysisResult llm = alarm.getAnalysisId() == null
            ? latestLlmByTaskId.get(alarm.getTaskId())
            : llmById.getOrDefault(alarm.getAnalysisId(), latestLlmByTaskId.get(alarm.getTaskId()));

        return LatestAlarmItemDTO.builder()
            .alarmId(alarm.getId())
            .taskId(alarm.getTaskId())
            .channelId(alarm.getChannelId())
            .channelName(channel == null ? formatChannelName(alarm.getChannelId()) : channel.getChannelName())
            .eventType(llm == null ? "未识别事件类型" : llm.getEventType())
            .eventSummary(llm == null ? emptyToDefault(alarm.getTriggerReason(), "暂无事件摘要") : emptyToDefault(llm.getEventSummary(), "暂无事件摘要"))
            .riskLevel(emptyToDefault(alarm.getAlarmLevel(), "未知"))
            .alarmStatus(emptyToDefault(alarm.getAlarmStatus(), "未知"))
            .triggerReason(alarm.getTriggerReason())
            .createTime(alarm.getCreateTime())
            .build();
    }

    private List<LatestTaskItemDTO> toTaskItems(List<AudioTask> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, RadioChannel> channelMap = channelMapper.selectBatchIds(
                tasks.stream()
                    .map(AudioTask::getChannelId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()
            ).stream()
            .collect(Collectors.toMap(RadioChannel::getId, Function.identity(), (a, b) -> a));

        return tasks.stream()
            .map(task -> {
                RadioChannel channel = task.getChannelId() == null ? null : channelMap.get(task.getChannelId());
                String error = emptyToDefault(task.getLastErrorMsg(), task.getErrorMsg());
                return LatestTaskItemDTO.builder()
                    .taskId(task.getId())
                    .channelId(task.getChannelId())
                    .channelName(channel == null ? formatChannelName(task.getChannelId()) : channel.getChannelName())
                    .taskType(task.getTaskType())
                    .taskStatus(task.getTaskStatus())
                    .riskLevel(task.getRiskLevel())
                    .errorMessage(error)
                    .createTime(task.getCreateTime())
                    .finishTime(task.getFinishTime())
                    .build();
            })
            .toList();
    }

    private boolean hasData(Object structuredData) {
        if (structuredData instanceof AlarmStatisticsDTO dto) {
            return dto.getTotalAlarmCount() > 0;
        }
        if (structuredData instanceof AlarmListDTO dto) {
            return dto.getTotalCount() > 0;
        }
        if (structuredData instanceof TaskStatusSummaryDTO dto) {
            return dto.getTotalTaskCount() > 0;
        }
        if (structuredData instanceof AlarmSummaryDTO dto) {
            return dto.getTotalAlarmCount() > 0 || dto.getTotalTaskCount() > 0;
        }
        if (structuredData instanceof AlarmTrendSummaryDTO dto) {
            return dto.getTotalAlarmCount() > 0;
        }
        return structuredData != null;
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private LlmAnalysisResult chooseLatest(LlmAnalysisResult left, LlmAnalysisResult right) {
        LocalDateTime leftTime = left == null ? null : left.getCreateTime();
        LocalDateTime rightTime = right == null ? null : right.getCreateTime();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private String emptyToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private String formatChannelName(Long channelId) {
        return channelId == null ? "未关联频道" : "频道#" + channelId;
    }
}
