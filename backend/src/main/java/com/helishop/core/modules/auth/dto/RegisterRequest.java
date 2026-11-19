package com.helishop.core.modules.auth.dto;

import com.helishop.core.common.constants.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin yêu cầu đăng ký tài khoản mới")
public class RegisterRequest {

    @Schema(description = "Địa chỉ Gmail nhận OTP", example = "customer@gmail.com")
    @NotBlank(message = "Gmail không được để trống")
    @Size(max = 150)
    @Pattern(regexp = "(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@gmail\\.com", message = "Vui lòng nhập địa chỉ @gmail.com")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Schema(description = "Mật khẩu bảo mật (tối thiểu 6 ký tự)", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    private String password;

    @Schema(description = "Họ và tên đầy đủ của người dùng (tùy chọn)", example = "Nguyễn Văn A")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "OTP không được để trống")
    @Pattern(regexp = "[0-9]{6}", message = "OTP phải có 6 chữ số")
    private String otp;

    @Schema(description = "Vai trò tài khoản (ROLE_CUSTOMER hoặc ROLE_SELLER)", example = "ROLE_CUSTOMER")
    @Builder.Default
    private UserRole role = UserRole.ROLE_CUSTOMER;
}
