package com.coast.radio.guard.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentReportRequestDTO {
    @NotBlank(message = "type 不能为空")
    private String type;
}

