package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证激活码请求DTO
 */
@Data
public class VerifyActivationCodeRequest {

    @NotBlank(message = "激活码不能为空")
    private String activationCode;
}
