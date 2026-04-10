package com.coast.radio.guard.vo.agent;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentReportVO {
    private String title;
    private String summary;
    private List<String> items;
    private Map<String, Object> providerInfo;
    private Map<String, Object> fallbackInfo;
}

