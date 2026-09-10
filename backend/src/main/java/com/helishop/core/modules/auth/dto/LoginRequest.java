package com.helishop.core.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Thông tin đăng nhập hệ thống")
public class LoginRequest {

    @Schema(description = "Email hoặc Số điện thoại đăng nhập", example = "vuvietanh@gmail.com hoặc 0988889999", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email hoặc Số điện thoại không được để trống")
    private String email;


    @Schema(description = "Mật khẩu", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
