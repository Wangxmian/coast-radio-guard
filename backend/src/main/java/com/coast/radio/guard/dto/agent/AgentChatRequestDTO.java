package com.coast.radio.guard.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentChatRequestDTO {
    @NotBlank(message = "message 不能为空")
    private String message;
}

