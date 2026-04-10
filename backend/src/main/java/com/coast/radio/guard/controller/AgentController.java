package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.agent.AgentChatRequestDTO;
import com.coast.radio.guard.dto.agent.AgentReportRequestDTO;
import com.coast.radio.guard.service.AgentService;
import com.coast.radio.guard.vo.agent.AgentChatVO;
import com.coast.radio.guard.vo.agent.AgentReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public Result<AgentChatVO> chat(@Valid @RequestBody AgentChatRequestDTO dto) {
        return Result.ok("智能体回复成功", agentService.chat(dto.getMessage()));
    }

    @PostMapping("/report")
    public Result<AgentReportVO> report(@Valid @RequestBody AgentReportRequestDTO dto) {
        return Result.ok("报告生成成功", agentService.report(dto.getType()));
    }
}

