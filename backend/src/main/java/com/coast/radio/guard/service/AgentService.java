package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.agent.AgentChatVO;
import com.coast.radio.guard.vo.agent.AgentReportVO;

public interface AgentService {
    AgentChatVO chat(String message);
    AgentReportVO report(String type);
}

