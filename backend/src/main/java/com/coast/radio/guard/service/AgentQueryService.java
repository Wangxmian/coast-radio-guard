package com.coast.radio.guard.service;

import com.coast.radio.guard.service.model.AgentIntentType;
import com.coast.radio.guard.service.model.AgentStructuredResult;

public interface AgentQueryService {
    AgentStructuredResult query(String question, AgentIntentType intent);
}
