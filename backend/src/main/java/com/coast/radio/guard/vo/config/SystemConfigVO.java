package com.coast.radio.guard.vo.config;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemConfigVO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
