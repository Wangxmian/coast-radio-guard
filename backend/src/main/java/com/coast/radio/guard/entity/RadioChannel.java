package com.coast.radio.guard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("radio_channel")
public class RadioChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("channel_code")
    private String channelCode;

    @TableField("channel_name")
    private String channelName;

    private String frequency;

    private Integer priority;

    private Integer status;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
