package com.helishop.core.modules.auth.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.auth.dto.*;
import com.helishop.core.modules.auth.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth/google") @RequiredArgsConstructor
@Tag(name = "Google sign-in", description = "Xác thực Gmail bằng Google Identity Services, không gửi OTP riêng")
public class GoogleAuthController {
    private static final String COOKIE = "helishop_google_nonce";
    private final GoogleTokenVerifier verifier;
    private final GoogleAuthService service;

    @GetMapping("/config")
    @Operation(summary = "Cấu hình Google công khai", description = "Chỉ trả client ID công khai và trạng thái bật; không trả client secret hoặc cấu hình email")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cấu hình runtime"))
    public ApiResponse<Config> config(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return ApiResponse.success(new Config(verifier.configured(), verifier.clientId()));
    }

    @PostMapping(value = "/challenge", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bắt đầu đăng nhập Google", description = "Sinh nonce TTL 5 phút, ràng buộc với cookie HttpOnly SameSite=Lax của trình duyệt. Dùng nonce khi khởi tạo Google SDK.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nonce và cookie"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Google chưa được cấu hình")})
    public ApiResponse<Challenge> challenge(HttpServletRequest request, HttpServletResponse response) {
        String nonce = service.challenge();
        cookie(request, response, nonce, 300);
        return ApiResponse.success(new Challenge(nonce));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Xác thực Google ID token", description = "Kiểm tra chữ ký Google RS256, issuer, audience, thời hạn, nonce và Gmail đã xác minh. Tài khoản liên kết được đăng nhập; tài khoản mới/chưa liên kết nhận ticket 5 phút, chưa cấp JWT.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng nhập hoặc cần hoàn tất tài khoản"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu/Gmail sai"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chứng thực/phiên không hợp lệ hoặc tài khoản bị khóa"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Định danh đã liên kết khác"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Google chưa được cấu hình")})
    public ApiResponse<GoogleAuthResult> login(@Valid @RequestBody GoogleLoginRequest body, @CookieValue(name = COOKIE, required = false) String nonce,
            HttpServletRequest request, HttpServletResponse response) {
        GoogleAuthResult result = service.login(body.credential(), nonce);
        if ("AUTHENTICATED".equals(result.status())) cookie(request, response, "", 0);
        return ApiResponse.success(result);
    }

    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chọn vai trò hoặc liên kết tài khoản cũ", description = "Ticket một lần ràng buộc cùng trình duyệt. User mới chỉ CUSTOMER/SELLER, không tạo mật khẩu giả. User cũ phải xác nhận mật khẩu hiện tại và giữ nguyên role/id/shop/đơn; tối đa 5 lần sai.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã hoàn tất và cấp phiên HeliShop"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Thiếu hoặc sai dữ liệu"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sai mật khẩu/phiên hoặc đã khóa"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không được tự cấp quyền admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Đăng ký/liên kết đồng thời"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Google chưa được cấu hình")})
    public ApiResponse<GoogleAuthResult> complete(@Valid @RequestBody GoogleCompleteRequest body, @CookieValue(name = COOKIE, required = false) String nonce,
            HttpServletRequest request, HttpServletResponse response) {
        GoogleAuthResult result = service.complete(body, nonce);
        cookie(request, response, "", 0);
        return ApiResponse.success(result);
    }

    private void cookie(HttpServletRequest request, HttpServletResponse response, String nonce, long maxAge) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, nonce).httpOnly(true).sameSite("Lax")
                .secure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))
                .path("/api/v1/auth/google").maxAge(maxAge).build().toString());
    }
    public record Config(boolean enabled, String clientId) {}
    public record Challenge(String nonce) {}
}
