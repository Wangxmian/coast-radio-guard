package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_analysis_result")
public class LlmAnalysisResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("event_type")
    private String eventType;

    @TableField("event_summary")
    private String eventSummary;

    private String reason;

    private String provider;

    @TableField("source_type")
    private String sourceType;

    @TableField("raw_response")
    private String rawResponse;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
