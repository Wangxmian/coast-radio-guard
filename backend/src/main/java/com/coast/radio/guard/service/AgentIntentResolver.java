package com.coast.radio.guard.service;

import com.coast.radio.guard.service.model.AgentIntentMatch;

public interface AgentIntentResolver {
    AgentIntentMatch resolve(String message);
}
