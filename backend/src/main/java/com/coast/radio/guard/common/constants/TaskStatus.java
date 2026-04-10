package com.coast.radio.guard.common.constants;

import java.util.Set;

public final class TaskStatus {

    private TaskStatus() {
    }

    public static final String WAITING = "WAITING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private static final Set<String> ALL = Set.of(WAITING, PROCESSING, SUCCESS, FAILED);

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
