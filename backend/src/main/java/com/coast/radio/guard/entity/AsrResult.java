package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asr_result")
public class AsrResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("transcript_text")
    private String transcriptText;

    @TableField("raw_transcript")
    private String rawTranscript;

    @TableField("corrected_transcript")
    private String correctedTranscript;

    @TableField("correction_diff")
    private String correctionDiff;

    @TableField("correction_provider")
    private String correctionProvider;

    @TableField("correction_fallback")
    private Integer correctionFallback;

    private BigDecimal confidence;

    private String language;

    private String provider;

    @TableField("source_type")
    private String sourceType;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
