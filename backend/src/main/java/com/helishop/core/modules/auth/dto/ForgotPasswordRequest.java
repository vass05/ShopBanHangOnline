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

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    @Schema(description = "Gmail hoặc Số điện thoại đăng ký tài khoản", example = "vuvietanh@gmail.com")
    private String email;
}
