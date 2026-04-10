package com.coast.radio.guard.service.impl;

import com.coast.radio.guard.service.AgentIntentResolver;
import com.coast.radio.guard.service.model.AgentIntentMatch;
import com.coast.radio.guard.service.model.AgentIntentType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AgentIntentResolverImpl implements AgentIntentResolver {

    @Override
    public AgentIntentMatch resolve(String message) {
        String text = normalize(message);
        if (text.isBlank()) {
            return match(AgentIntentType.UNKNOWN, 0.1D, "empty");
        }

        if (containsAny(text, "日报", "值守摘要", "今日摘要", "总结今天", "今日告警情况", "今日情况", "值守总结", "总结")) {
            return match(AgentIntentType.DAILY_SUMMARY, 0.98D, "summary_keywords");
        }

        if ((containsAny(text, "趋势", "变化", "最近7天", "最近七天", "最近一周", "七天", "7天", "一周"))
            && containsAny(text, "告警", "高风险")) {
            return match(AgentIntentType.ALARM_TREND, 0.97D, "trend_keywords");
        }

        if (containsAny(text, "未处理", "待处理", "未处置") && containsAny(text, "告警", "事件")) {
            return match(AgentIntentType.UNHANDLED_ALARMS, 0.97D, "unhandled_alarm_keywords");
        }

        if (containsAny(text, "最新", "最近", "列出", "列表")
            && containsAny(text, "高风险", "高危", "high")
            && containsAny(text, "事件", "告警")) {
            return match(AgentIntentType.LATEST_HIGH_RISK_EVENTS, 0.96D, "high_risk_list_keywords");
        }

        if (containsAny(text, "最近5条", "最近五条", "最新5条", "最新五条", "最近告警", "最新告警")
            && containsAny(text, "告警", "事件")) {
            return match(AgentIntentType.RECENT_ALARMS, 0.95D, "recent_alarm_keywords");
        }

        if (containsAny(text, "任务")
            && containsAny(text, "执行情况", "完成情况", "失败", "状态", "进度")) {
            return match(AgentIntentType.TASK_STATUS, 0.95D, "task_status_keywords");
        }

        if (containsAny(text, "告警", "高风险")
            && containsAny(text, "多少", "几条", "统计", "占比", "数量")) {
            return match(AgentIntentType.ALARM_STATS, 0.94D, "alarm_stats_keywords");
        }

        return match(AgentIntentType.UNKNOWN, 0.25D, "unmatched");
    }

    private AgentIntentMatch match(AgentIntentType intent, double confidence, String rule) {
        return AgentIntentMatch.builder()
            .intent(intent)
            .confidence(confidence)
            .rule(rule)
            .build();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("？", "")
            .replace("?", "")
            .replace("，", "")
            .replace(",", "")
            .replace("。", "")
            .replace(".", "")
            .replace("：", "")
            .replace(":", "")
            .replace(" ", "");
    }
}
