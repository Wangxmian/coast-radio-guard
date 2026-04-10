package com.coast.radio.guard.dto.audio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AudioTaskCreateDTO {

    @NotNull(message = "频道ID不能为空")
    private Long channelId;

    @NotBlank(message = "原始音频路径不能为空")
    private String originalFilePath;

    private String enhancedFilePath;

    private BigDecimal duration;

    private String errorMsg;
}
