package com.coast.radio.guard.common.constants;

import java.util.Map;
import java.util.Set;

public final class AlarmFlow {

    private AlarmFlow() {
    }

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        AlarmStatus.UNHANDLED, Set.of(AlarmStatus.ACKNOWLEDGED, AlarmStatus.FALSE_ALARM),
        AlarmStatus.ACKNOWLEDGED, Set.of(AlarmStatus.PROCESSING, AlarmStatus.FALSE_ALARM),
        AlarmStatus.PROCESSING, Set.of(AlarmStatus.RESOLVED, AlarmStatus.FALSE_ALARM),
        AlarmStatus.RESOLVED, Set.of(AlarmStatus.CLOSED)
    );

    public static boolean canTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.get(fromStatus);
        return allowed != null && allowed.contains(toStatus);
    }
}
