package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EntityResultVO {
    private Long id;
    private Long taskId;
    private String entityType;
    private String entityValue;
    private BigDecimal confidence;
    private LocalDateTime createTime;
}
