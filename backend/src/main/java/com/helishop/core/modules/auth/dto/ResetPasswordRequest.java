package com.helishop.core.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đặt lại mật khẩu với mã xác thực OTP")
public class ResetPasswordRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    @Schema(description = "Gmail hoặc Số điện thoại đăng ký tài khoản", example = "vuvietanh@gmail.com")
    private String email;

    @NotBlank(message = "Mã xác thực OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 chữ số")
    @Schema(description = "Mã OTP xác thực 6 chữ số", example = "123456")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có tối thiểu 6 ký tự")
    @Schema(description = "Mật khẩu mới", example = "NewPassword123!")
    private String newPassword;
}
