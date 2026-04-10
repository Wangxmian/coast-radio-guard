package com.coast.radio.guard.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeEnhanceRequestDTO {
    private Long taskId;
    private String originalFilePath;
}
