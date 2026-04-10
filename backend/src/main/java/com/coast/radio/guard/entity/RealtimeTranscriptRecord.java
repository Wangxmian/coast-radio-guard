package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("realtime_transcript_record")
public class RealtimeTranscriptRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("task_id")
    private Long taskId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("raw_transcript")
    private String rawTranscript;

    @TableField("corrected_transcript")
    private String correctedTranscript;

    @TableField("start_time_ms")
    private Integer startTimeMs;

    @TableField("end_time_ms")
    private Integer endTimeMs;

    @TableField("is_final")
    private Integer isFinal;

    @TableField("has_speech")
    private Integer hasSpeech;

    @TableField("analysis_id")
    private Long analysisId;

    @TableField("alarm_id")
    private Long alarmId;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("event_type")
    private String eventType;

    @TableField("create_time")
    private LocalDateTime createTime;
}
