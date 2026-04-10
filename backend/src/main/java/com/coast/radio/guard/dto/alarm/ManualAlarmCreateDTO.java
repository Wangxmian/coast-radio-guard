package com.coast.radio.guard.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualAlarmCreateDTO {

    private Long taskId;
    private Long channelId;

    @NotBlank(message = "alarmLevel不能为空")
    private String alarmLevel;

    @NotBlank(message = "triggerReason不能为空")
    private String triggerReason;

    private String remark;
}
