package com.bank.loan.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROD_ERROR_MESSAGE = "系统繁忙，请稍后重试";

    @Value("${app.debug:false}")
    private boolean debugMode;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException e) {
        logException(e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(403, "无权限访问"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<String>> handleAuthenticationException(AuthenticationException e) {
        logException(e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "未认证或token无效"));
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        logException(e);
        return Result.error(resolveErrorMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        logException(e);
        return Result.error(resolveErrorMessage(e));
    }

    private String resolveErrorMessage(Throwable e) {
        if (debugMode) {
            return e.getMessage();
        }
        return PROD_ERROR_MESSAGE;
    }

    private void logException(Throwable e) {
        if (debugMode) {
            LOGGER.warn("发生异常: {}", e.getMessage(), e);
        } else {
            LOGGER.error("发生异常: {}", e.getMessage(), e);
        }
    }
}
