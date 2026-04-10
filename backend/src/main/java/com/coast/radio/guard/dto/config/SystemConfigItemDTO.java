package com.coast.radio.guard.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigItemDTO {

    @NotBlank(message = "configKey不能为空")
    private String configKey;

    private String configValue;
}
