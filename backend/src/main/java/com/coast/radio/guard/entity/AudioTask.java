package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("audio_task")
public class AudioTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("channel_id")
    private Long channelId;

    @TableField("original_file_path")
    private String originalFilePath;

    @TableField("enhanced_file_path")
    private String enhancedFilePath;

    @TableField("task_type")
    private String taskType;

    @TableField("source_session_id")
    private String sourceSessionId;

    @TableField("task_status")
    private String taskStatus;

    @TableField("se_status")
    private String seStatus;

    @TableField("asr_status")
    private String asrStatus;

    @TableField("llm_status")
    private String llmStatus;

    @TableField("transcript_text")
    private String transcriptText;

    @TableField("risk_level")
    private String riskLevel;

    private BigDecimal duration;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("last_error_msg")
    private String lastErrorMsg;

    @TableField("execute_time")
    private LocalDateTime executeTime;

    @TableField("last_transcript_time")
    private LocalDateTime lastTranscriptTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
