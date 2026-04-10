package com.coast.radio.guard.dto.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SystemConfigBatchUpdateDTO {

    @NotEmpty(message = "configs不能为空")
    @Valid
    private List<SystemConfigItemDTO> configs;
}
