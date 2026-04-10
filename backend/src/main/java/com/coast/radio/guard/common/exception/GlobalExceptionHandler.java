package com.coast.radio.guard.common.exception;

import com.coast.radio.guard.common.api.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Deprecated
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException e) {
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        return ApiResponse.fail("400", e.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleReadable(HttpMessageNotReadableException e) {
        return ApiResponse.fail("400", "请求体格式错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleDenied(AccessDeniedException e) {
        return ApiResponse.fail("403", "无访问权限");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleEx(Exception e) {
        return ApiResponse.fail("500", "系统繁忙，请稍后重试");
    }
}
