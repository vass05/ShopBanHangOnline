package com.helishop.core.modules.auth.dto;

import com.helishop.core.common.constants.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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

    @Schema(description = "Địa chỉ email duy nhất của người dùng", example = "customer@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Schema(description = "Mật khẩu bảo mật (tối thiểu 6 ký tự)", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    private String password;

    @Schema(description = "Họ và tên đầy đủ của người dùng", example = "Nguyễn Văn A", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Schema(description = "Số điện thoại liên lạc", example = "0987654321")
    private String phone;

    @Schema(description = "Vai trò tài khoản (ROLE_CUSTOMER hoặc ROLE_SELLER)", example = "ROLE_CUSTOMER")
    @Builder.Default
    private UserRole role = UserRole.ROLE_CUSTOMER;
}
