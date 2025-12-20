package com.ems.exception;

import java.util.List;

/**
 * 验证异常类
 * 用于处理数据验证中的异常情况
 *
 * @author EMS Team
 */
public class ValidationException extends RuntimeException {

    private final String errorCode;
    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.validationErrors = List.of(message);
    }

    public ValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.validationErrors = List.of(message);
    }

    public ValidationException(String message, List<String> validationErrors) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.validationErrors = validationErrors;
    }

    public ValidationException(String errorCode, String message, List<String> validationErrors) {
        super(message);
        this.errorCode = errorCode;
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VALIDATION_ERROR";
        this.validationErrors = List.of(message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public String toString() {
        return "ValidationException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                ", validationErrors=" + validationErrors +
                '}';
    }
}