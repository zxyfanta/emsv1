package com.ems.exception;

/**
 * 系统错误码枚举
 * 统一管理系统中的错误码和错误信息
 *
 * @author EMS Team
 */
public enum ErrorCode {

    // 通用错误码 (1000-1999)
    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(1000, "系统内部错误"),
    INVALID_PARAMETER(1001, "参数校验失败"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    PERMISSION_DENIED(1003, "权限不足"),
    OPERATION_NOT_ALLOWED(1004, "操作不被允许"),
    DATA_CONFLICT(1005, "数据冲突"),

    // 用户相关错误码 (2000-2999)
    USER_NOT_FOUND(2000, "用户不存在"),
    USER_ALREADY_EXISTS(2001, "用户已存在"),
    INVALID_CREDENTIALS(2002, "用户名或密码错误"),
    USER_DISABLED(2003, "用户已被禁用"),
    USER_LOCKED(2004, "用户已被锁定"),
    PASSWORD_EXPIRED(2005, "密码已过期"),

    // 设备相关错误码 (3000-3999)
    DEVICE_NOT_FOUND(3000, "设备不存在"),
    DEVICE_ALREADY_EXISTS(3001, "设备已存在"),
    DEVICE_OFFLINE(3002, "设备离线"),
    DEVICE_BUSY(3003, "设备忙碌"),
    INVALID_DEVICE_TYPE(3004, "无效的设备类型"),
    DEVICE_CONFIGURATION_ERROR(3005, "设备配置错误"),

    // 企业相关错误码 (4000-4999)
    ENTERPRISE_NOT_FOUND(4000, "企业不存在"),
    ENTERPRISE_ALREADY_EXISTS(4001, "企业已存在"),
    ENTERPRISE_DISABLED(4002, "企业已被禁用"),

    // 数据相关错误码 (5000-5999)
    DATA_VALIDATION_ERROR(5000, "数据验证失败"),
    DATA_FORMAT_ERROR(5001, "数据格式错误"),
    DATA_DUPLICATE(5002, "数据重复"),
    DATA_OUT_OF_RANGE(5003, "数据超出范围"),

    // 网络相关错误码 (6000-6999)
    NETWORK_ERROR(6000, "网络连接错误"),
    CONNECTION_TIMEOUT(6001, "连接超时"),
    MQTT_CONNECTION_ERROR(6002, "MQTT连接失败"),
    MQTT_PUBLISH_ERROR(6003, "MQTT消息发布失败"),

    // 业务相关错误码 (7000-7999)
    ALERT_RULE_NOT_FOUND(7000, "告警规则不存在"),
    ALERT_RULE_CONFLICT(7001, "告警规则冲突"),
    DEVICE_GROUP_NOT_FOUND(7002, "设备分组不存在"),
    DEVICE_GROUP_CONFLICT(7003, "设备分组冲突"),

    // 文件相关错误码 (8000-8999)
    FILE_NOT_FOUND(8000, "文件不存在"),
    FILE_UPLOAD_ERROR(8001, "文件上传失败"),
    FILE_FORMAT_ERROR(8002, "文件格式错误"),
    FILE_SIZE_EXCEEDED(8003, "文件大小超限"),

    // 认证授权错误码 (9000-9999)
    TOKEN_INVALID(9000, "Token无效"),
    TOKEN_EXPIRED(9001, "Token已过期"),
    TOKEN_MISSING(9002, "Token缺失"),
    UNAUTHORIZED(9003, "未授权访问"),
    ACCESS_DENIED(9004, "访问被拒绝");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码查找对应的枚举
     *
     * @param code 错误码
     * @return 对应的ErrorCode枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}