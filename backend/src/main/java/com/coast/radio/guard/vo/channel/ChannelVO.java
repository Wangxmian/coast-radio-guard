package com.coast.radio.guard.vo.channel;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChannelVO {

    private Long id;
    private String channelCode;
    private String channelName;
    private String frequency;
    private Integer priority;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
