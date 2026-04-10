package com.coast.radio.guard.dto.realtime;

import lombok.Data;

@Data
public class RealtimeStartRequestDTO {
    private Long channelId;
    private String mode;
}
