package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RiskEventVO {
    private Long id;
    private Long taskId;
    private Long analysisId;
    private String riskLevel;
    private String eventType;
    private String summary;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
