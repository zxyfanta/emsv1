package com.cdutetc.ems.util;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一API响应格式工具类
 */
@Data
public class ApiResponse<T> {
    private Integer status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error(Integer status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "创建成功", data);
    }

    public static <T> ApiResponse<T> updated(T data) {
        return new ApiResponse<>(200, "更新成功", data);
    }

    public static <T> ApiResponse<T> deleted() {
        return new ApiResponse<>(200, "删除成功", null);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    public static <T> ApiResponse<T> badRequest(String message, T data) {
        return new ApiResponse<>(400, message, data);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message, null);
    }
}