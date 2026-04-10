package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AsrResultVO {
    private Long id;
    private Long taskId;
    private String transcriptText;
    private String rawTranscript;
    private String correctedTranscript;
    private String correctionDiff;
    private String correctionProvider;
    private Boolean correctionFallback;
    private BigDecimal confidence;
    private String language;
    private String provider;
    private String sourceType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
