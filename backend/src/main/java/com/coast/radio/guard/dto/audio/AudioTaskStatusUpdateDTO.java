package com.coast.radio.guard.dto.audio;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AudioTaskStatusUpdateDTO {

    @NotBlank(message = "任务状态不能为空")
    private String taskStatus;

    private String errorMsg;
}
