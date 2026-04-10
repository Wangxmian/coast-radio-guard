package com.coast.radio.guard.vo.audio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AudioTaskVO {

    private Long id;
    private Long channelId;
    private String originalFilePath;
    private String enhancedFilePath;
    private String taskType;
    private String sourceSessionId;
    private String taskStatus;
    private String seStatus;
    private String asrStatus;
    private String llmStatus;
    private String transcriptText;
    private String riskLevel;
    private BigDecimal duration;
    private String errorMsg;
    private String lastErrorMsg;
    private LocalDateTime executeTime;
    private LocalDateTime lastTranscriptTime;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private LocalDateTime updateTime;
}
