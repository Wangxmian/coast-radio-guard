package com.coast.radio.guard.service;

import com.coast.radio.guard.service.model.AgentStructuredResult;

public interface AgentAnswerFormatter {
    String format(String question, AgentStructuredResult result);

    String formatUnsupported(String question);
}
