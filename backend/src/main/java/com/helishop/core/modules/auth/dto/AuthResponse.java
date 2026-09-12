package com.helishop.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helishop.core.common.constants.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin xác thực và cặp Token truy cập")
public class AuthResponse {

    @Schema(description = "Access Token (JWT HMAC-SHA256, hạn 15 phút)", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String accessToken;

    @Schema(description = "Refresh Token (UUID lưu Redis, hạn 7 ngày)", example = "b7c258d4a7...")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Loại Token", example = "Bearer")
    private String tokenType = "Bearer";

    @Builder.Default
    @Schema(description = "Thời gian hết hạn Access Token (giây)", example = "900")
    private Long expiresIn = 900L;

    @Schema(description = "ID người dùng", example = "1")
    private Long userId;

    @Schema(description = "Email đăng nhập", example = "user@helishop.com")
    private String email;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    private String phone;

    @Schema(description = "Vai trò người dùng trong hệ thống", example = "ROLE_CUSTOMER")
    private UserRole role;

    // Backward compatibility helper
    public String getToken() {
        return accessToken;
    }

    public void setToken(String token) {
        this.accessToken = token;
    }
}
