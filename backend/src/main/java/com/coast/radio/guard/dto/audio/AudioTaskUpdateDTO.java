package com.coast.radio.guard.dto.audio;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AudioTaskUpdateDTO {

    @NotNull(message = "频道ID不能为空")
    private Long channelId;

    private String originalFilePath;
    private String enhancedFilePath;
    private BigDecimal duration;
    private String errorMsg;
}
