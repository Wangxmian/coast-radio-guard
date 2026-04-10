package com.coast.radio.guard.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChannelCreateDTO {

    @NotBlank(message = "频道编码不能为空")
    private String channelCode;

    @NotBlank(message = "频道名称不能为空")
    private String channelName;

    @NotBlank(message = "频段不能为空")
    private String frequency;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
