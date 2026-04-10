package com.coast.radio.guard.service.impl;

import com.coast.radio.guard.service.AgentAnswerFormatter;
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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AgentAnswerFormatterImpl implements AgentAnswerFormatter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M月d日");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Override
    public String format(String question, AgentStructuredResult result) {
        if (result == null || !result.isSupported()) {
            return formatUnsupported(question);
        }
        return switch (result.getIntent()) {
            case ALARM_STATS -> formatAlarmStats((AlarmStatisticsDTO) result.getStructuredData());
            case LATEST_HIGH_RISK_EVENTS -> formatAlarmList((AlarmListDTO) result.getStructuredData(), "当前暂无高风险告警事件。");
            case UNHANDLED_ALARMS -> formatUnhandled((AlarmListDTO) result.getStructuredData());
            case RECENT_ALARMS -> formatAlarmList((AlarmListDTO) result.getStructuredData(), "当前暂无告警记录。");
            case TASK_STATUS -> formatTaskStatus((TaskStatusSummaryDTO) result.getStructuredData());
            case DAILY_SUMMARY -> formatDailySummary((AlarmSummaryDTO) result.getStructuredData());
            case ALARM_TREND -> formatTrend((AlarmTrendSummaryDTO) result.getStructuredData());
            case UNKNOWN -> formatUnsupported(question);
        };
    }

    @Override
    public String formatUnsupported(String question) {
        return "当前这个问题还不能直接从值守数据库中准确回答。\n"
            + "你可以尝试询问：今天有多少条告警、当前有哪些未处理告警、最近5条告警是什么、今天任务执行情况如何、帮我生成今日值守摘要。";
    }

    private String formatAlarmStats(AlarmStatisticsDTO dto) {
        if (dto == null || dto.getTotalAlarmCount() <= 0) {
            return "按系统当前时区统计，今日暂无告警数据。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("截至目前，系统今日共记录告警 ")
            .append(dto.getTotalAlarmCount())
            .append(" 条，其中高风险告警 ")
            .append(dto.getHighRiskAlarmCount())
            .append(" 条，占比约 ")
            .append(formatRatio(dto.getHighRiskRatio()))
            .append("。");
        if (dto.getLatestHighRiskAlarm() != null) {
            LatestAlarmItemDTO latest = dto.getLatestHighRiskAlarm();
            sb.append("\n最近一条高风险事件发生于 ")
                .append(formatDateTime(latest.getCreateTime()))
                .append("，关联频道为“")
                .append(latest.getChannelName())
                .append("”，当前状态为“")
                .append(toAlarmStatusText(latest.getAlarmStatus()))
                .append("”。");
        }
        return sb.toString();
    }

    private String formatAlarmList(AlarmListDTO dto, String emptyText) {
        if (dto == null || dto.getTotalCount() <= 0 || dto.getItems() == null || dto.getItems().isEmpty()) {
            return emptyText;
        }
        List<String> lines = new ArrayList<>();
        lines.add("已为你整理 " + dto.getScope() + "，当前共返回 " + dto.getItems().size() + " 条：");
        int index = 1;
        for (LatestAlarmItemDTO item : dto.getItems()) {
            lines.add(index + ". " + formatAlarmLine(item));
            index++;
        }
        return String.join("\n", lines);
    }

    private String formatUnhandled(AlarmListDTO dto) {
        if (dto == null || dto.getTotalCount() <= 0) {
            return "当前没有待处理告警，值守面板里未发现处于“待处理”状态的记录。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("当前共有 " + dto.getTotalCount() + " 条未处理告警。以下先列出最近 " + dto.getItems().size() + " 条，便于你快速核查：");
        int index = 1;
        for (LatestAlarmItemDTO item : dto.getItems()) {
            lines.add(index + ". " + formatAlarmLine(item));
            index++;
        }
        return String.join("\n", lines);
    }

    private String formatTaskStatus(TaskStatusSummaryDTO dto) {
        if (dto == null || dto.getTotalTaskCount() <= 0) {
            return "今天还没有查询到音频任务记录，当前无法生成任务执行概况。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("今天共记录音频任务 " + dto.getTotalTaskCount() + " 条，其中已完成 "
            + dto.getSuccessTaskCount() + " 条，执行失败 " + dto.getFailedTaskCount() + " 条，处理中 "
            + dto.getProcessingTaskCount() + " 条，等待执行 " + dto.getWaitingTaskCount() + " 条。");
        lines.add("按已完成口径估算，当前完成率约为 " + formatRatio(dto.getCompletionRate()) + "。");
        if (dto.getFailedTaskCount() > 0 && dto.getFailedTasks() != null && !dto.getFailedTasks().isEmpty()) {
            lines.add("最近失败任务如下：");
            int index = 1;
            for (LatestTaskItemDTO task : dto.getFailedTasks()) {
                lines.add(index + ". 任务#" + task.getTaskId()
                    + "，频道“" + task.getChannelName() + "”，状态“" + toTaskStatusText(task.getTaskStatus()) + "”"
                    + appendIfPresent(task.getErrorMessage(), "，失败原因：")
                    + appendIfPresent(formatDateTime(task.getCreateTime()), "，创建时间："));
                index++;
            }
        }
        return String.join("\n", lines);
    }

    private String formatDailySummary(AlarmSummaryDTO dto) {
        if (dto == null || (dto.getTotalAlarmCount() <= 0 && dto.getTotalTaskCount() <= 0)) {
            return "今天暂未查询到值守相关业务数据，当前无法生成今日摘要。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("今日值守摘要如下：");
        lines.add("1. 今日共记录告警 " + dto.getTotalAlarmCount() + " 条，其中高风险告警 "
            + dto.getHighRiskAlarmCount() + " 条，仍待处理 " + dto.getUnhandledAlarmCount() + " 条。");
        lines.add("2. 今日共生成音频任务 " + dto.getTotalTaskCount() + " 条，已完成 "
            + dto.getSuccessTaskCount() + " 条，失败 " + dto.getFailedTaskCount() + " 条。");
        if (dto.getLatestHighRiskAlarm() != null) {
            lines.add("3. 最近一条高风险告警为：" + formatAlarmLine(dto.getLatestHighRiskAlarm()));
        }
        if (dto.getRecentUnhandledAlarms() != null && !dto.getRecentUnhandledAlarms().isEmpty()) {
            lines.add("4. 当前仍需关注的未处理告警主要有 " + dto.getRecentUnhandledAlarms().size() + " 条，我已放在明细数据里，前端可继续展开查看。");
        }
        return String.join("\n", lines);
    }

    private String formatTrend(AlarmTrendSummaryDTO dto) {
        if (dto == null || dto.getTotalAlarmCount() <= 0) {
            return "最近 7 天暂无告警数据，暂时无法判断趋势变化。";
        }
        List<String> lines = new ArrayList<>();
        lines.add("最近 7 天共记录告警 " + dto.getTotalAlarmCount() + " 条，其中高风险告警 "
            + dto.getTotalHighRiskCount() + " 条。");
        if (dto.getPeakAlarmDate() != null) {
            lines.add("告警峰值出现在 " + formatDate(dto.getPeakAlarmDate()) + "，当天共 " + dto.getPeakAlarmCount() + " 条。");
        }
        lines.add("按天查看：");
        for (AlarmTrendPointDTO point : dto.getPoints()) {
            lines.add("- " + formatDate(point.getDate()) + "：告警 " + point.getAlarmCount() + " 条，高风险 " + point.getHighRiskCount() + " 条");
        }
        return String.join("\n", lines);
    }

    private String formatAlarmLine(LatestAlarmItemDTO item) {
        return formatDateTime(item.getCreateTime())
            + " | 频道“" + item.getChannelName() + "”"
            + " | 任务#" + fallbackNumber(item.getTaskId())
            + " | 事件：" + item.getEventType()
            + " | 风险等级：" + toRiskText(item.getRiskLevel())
            + " | 状态：" + toAlarmStatusText(item.getAlarmStatus());
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FMT.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATETIME_FMT.format(dateTime);
    }

    private String toAlarmStatusText(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "UNHANDLED" -> "待处理";
            case "ACKNOWLEDGED" -> "已确认";
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "CLOSED" -> "已关闭";
            case "FALSE_ALARM" -> "误报";
            default -> status;
        };
    }

    private String toTaskStatusText(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "WAITING" -> "待执行";
            case "PROCESSING" -> "处理中";
            case "SUCCESS" -> "已完成";
            case "FAILED" -> "执行失败";
            default -> status;
        };
    }

    private String toRiskText(String riskLevel) {
        if (riskLevel == null) {
            return "未知";
        }
        return switch (riskLevel) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> riskLevel;
        };
    }

    private String formatRatio(double ratio) {
        return String.format("%.1f%%", ratio);
    }

    private String appendIfPresent(String value, String prefix) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return prefix + value;
    }

    private String fallbackNumber(Long value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
