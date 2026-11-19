package com.helishop.core.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu gửi mã OTP quên mật khẩu")
public class ForgotPasswordRequest {

    @NotBlank(message = "Gmail không được để trống")
    @Schema(description = "Gmail đăng ký tài khoản", example = "vuvietanh@gmail.com")
    @jakarta.validation.constraints.Email
    private String email;
}
