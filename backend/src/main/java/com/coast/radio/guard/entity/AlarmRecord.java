package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_record")
public class AlarmRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("analysis_id")
    private Long analysisId;

    @TableField("risk_event_id")
    private Long riskEventId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("alarm_level")
    private String alarmLevel;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("trigger_reason")
    private String triggerReason;

    @TableField("alarm_status")
    private String alarmStatus;

    @TableField("is_auto_created")
    private Integer isAutoCreated;

    @TableField("handle_user")
    private String handleUser;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
