package com.coast.radio.guard.common.constants;

public final class ResultCode {

    private ResultCode() {
    }

    public static final String SUCCESS = "0";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String NOT_FOUND = "404";
    public static final String SERVER_ERROR = "500";

    public static final String LOGIN_FAILED = "1001";
    public static final String USER_DISABLED = "1002";
    public static final String RESOURCE_NOT_FOUND = "1003";
}
