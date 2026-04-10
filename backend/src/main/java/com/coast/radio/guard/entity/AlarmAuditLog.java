package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_audit_log")
public class AlarmAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alarm_id")
    private Long alarmId;

    @TableField("action_type")
    private String actionType;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operator_user_id")
    private Long operatorUserId;

    @TableField("operator_username")
    private String operatorUsername;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}
