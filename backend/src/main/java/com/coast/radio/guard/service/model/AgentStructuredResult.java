package com.coast.radio.guard.service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentStructuredResult {
    private AgentIntentType intent;
    private boolean supported;
    private boolean hasData;
    private Object structuredData;
}
