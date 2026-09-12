package com.helishop.core.modules.user.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.user.dto.AddressRequest;
import com.helishop.core.modules.user.dto.ChangePasswordRequest;
import com.helishop.core.modules.user.dto.UpdateProfileRequest;
import com.helishop.core.modules.user.dto.UserAddressResponse;
import com.helishop.core.modules.user.dto.UserResponse;
import com.helishop.core.modules.user.service.UserService;
import com.helishop.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Customer Profile Management", description = "APIs quản lý hồ sơ cá nhân, đổi mật khẩu và sổ địa chỉ giao hàng khách hàng")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin hồ sơ người dùng hiện tại",
            description = "Trả về thông tin chi tiết tài khoản của người dùng đã đăng nhập (họ tên, email, sđt, avatar, vai trò)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc phiên hết hạn",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UserResponse> getMyProfile() {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.getMyProfile(userId), "Lấy thông tin hồ sơ thành công");
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật thông tin cá nhân",
            description = "Cập nhật họ và tên, số điện thoại hoặc ảnh đại diện cho tài khoản hiện tại"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc số điện thoại đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.updateProfile(userId, request), "Cập nhật thông tin cá nhân thành công");
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Đổi mật khẩu người dùng",
            description = "Xác minh mật khẩu hiện tại bằng BCrypt và cập nhật mật khẩu mới"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại sai hoặc xác nhận mật khẩu không khớp",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId(null);
        userService.changePassword(userId, request);
        return ApiResponse.success(null, "Đổi mật khẩu thành công! Vui lòng ghi nhớ mật khẩu mới.");
    }

    @GetMapping("/me/addresses")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách sổ địa chỉ nhận hàng",
            description = "Trả về toàn bộ danh sách địa chỉ nhận hàng của người dùng, sắp xếp ưu tiên địa chỉ mặc định lên đầu"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách địa chỉ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<UserAddressResponse>> getUserAddresses() {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.getUserAddresses(userId), "Lấy danh sách địa chỉ thành công");
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Thêm mới địa chỉ nhận hàng",
            description = "Tạo địa chỉ nhận hàng mới. Nếu là địa chỉ đầu tiên hoặc được tick chọn mặc định, tự động set làm mặc định"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Thêm địa chỉ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UserAddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.addAddress(userId, request), "Thêm địa chỉ nhận hàng thành công");
    }

    @PutMapping("/me/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật địa chỉ nhận hàng",
            description = "Chỉnh sửa thông tin địa chỉ nhận hàng của người dùng"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật địa chỉ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UserAddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.updateAddress(userId, id, request), "Cập nhật địa chỉ thành công");
    }

    @DeleteMapping("/me/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xóa địa chỉ nhận hàng",
            description = "Xóa một địa chỉ khỏi sổ địa chỉ. Nếu xóa địa chỉ mặc định, tự động chuyển địa chỉ gần nhất còn lại làm mặc định"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa địa chỉ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId(null);
        userService.deleteAddress(userId, id);
        return ApiResponse.success(null, "Xóa địa chỉ nhận hàng thành công");
    }

    @PutMapping("/me/addresses/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Thiết lập địa chỉ mặc định",
            description = "Đặt địa chỉ được chọn làm địa chỉ giao hàng mặc định"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thiết lập mặc định thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UserAddressResponse> setDefaultAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId(null);
        return ApiResponse.success(userService.setDefaultAddress(userId, id), "Thiết lập địa chỉ mặc định thành công");
    }
}
