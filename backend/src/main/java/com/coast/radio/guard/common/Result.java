package com.coast.radio.guard.common;

import com.coast.radio.guard.common.constants.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private boolean success;

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS, "OK", data, true);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS, message, data, true);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null, false);
    }
}
