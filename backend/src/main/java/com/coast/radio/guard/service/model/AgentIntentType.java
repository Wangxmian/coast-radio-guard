package com.coast.radio.guard.service.model;

public enum AgentIntentType {
    ALARM_STATS("alarm_stats", "告警统计"),
    LATEST_HIGH_RISK_EVENTS("latest_high_risk_events", "最新高风险事件"),
    UNHANDLED_ALARMS("unhandled_alarms", "未处理告警"),
    RECENT_ALARMS("recent_alarms", "最近告警"),
    TASK_STATUS("task_status", "任务状态"),
    DAILY_SUMMARY("daily_summary", "值守摘要"),
    ALARM_TREND("alarm_trend", "告警趋势"),
    UNKNOWN("unknown", "未知问题");

    private final String code;
    private final String description;

    AgentIntentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
