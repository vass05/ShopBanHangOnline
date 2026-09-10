package com.helishop.core.modules.cart.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.cart.dto.AddToCartRequest;
import com.helishop.core.modules.cart.dto.CartResponse;
import com.helishop.core.modules.cart.dto.RemoveCartItemsRequest;
import com.helishop.core.modules.cart.dto.UpdateCartItemRequest;
import com.helishop.core.modules.cart.service.RedisCartService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Engine Management", description = "APIs quản lý giỏ hàng Shopee hiệu năng cao trên Redis Hash")
public class CartController {

    private final RedisCartService cartService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xem giỏ hàng người dùng (Gom nhóm theo Shop)",
            description = "Đọc toàn bộ giỏ hàng từ Redis Hash key 'cart:user:{userId}', tự động tính toán tổng tiền phụ theo từng Shop và tổng thanh toán toàn giỏ hàng"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy giỏ hàng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực danh tính",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CartResponse> getCart(
            @RequestParam(required = false) Long customerId) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(cartService.getCartGroupedByShop(userId), "Lấy thông tin giỏ hàng thành công");
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Thêm biến thể sản phẩm (SKU) vào giỏ hàng",
            description = "Thêm mới hoặc cộng dồn số lượng biến thể sản phẩm vào giỏ hàng Redis Hash, tự động kiểm tra tồn kho khả dụng và đặt TTL 30 ngày"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Thêm vào giỏ hàng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy biến thể sản phẩm (SKU)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số lượng trong kho không đủ đáp ứng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CartResponse> addToCart(
            @RequestParam(required = false) Long customerId,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(cartService.addToCart(userId, request), "Thêm sản phẩm vào giỏ hàng thành công");
    }

    @PutMapping("/items/{skuId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật số lượng sản phẩm trong giỏ hàng",
            description = "Cập nhật số lượng của một SKU. Nếu số lượng = 0, sản phẩm sẽ tự động được xóa khỏi giỏ hàng. Tự động gia hạn TTL 30 ngày."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật số lượng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Số lượng không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ hàng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số lượng trong kho không đủ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long skuId,
            @RequestParam(required = false) Long customerId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(cartService.updateItemQuantity(userId, skuId, request.getQuantity()), "Cập nhật số lượng thành công");
    }

    @DeleteMapping("/items/{skuId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xóa một sản phẩm khỏi giỏ hàng",
            description = "Xóa một biến thể SKU cụ thể ra khỏi Redis Hash giỏ hàng"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa sản phẩm khỏi giỏ thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CartResponse> removeItem(
            @PathVariable Long skuId,
            @RequestParam(required = false) Long customerId) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(cartService.removeItemsFromCart(userId, List.of(skuId)), "Xóa sản phẩm khỏi giỏ hàng thành công");
    }

    @DeleteMapping("/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xóa nhiều sản phẩm đã chọn khỏi giỏ hàng",
            description = "Xóa danh sách các SKU được gửi lên qua lệnh HDEL trên Redis"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa các sản phẩm thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<CartResponse> removeMultipleItems(
            @RequestParam(required = false) Long customerId,
            @Valid @RequestBody RemoveCartItemsRequest request) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        return ApiResponse.success(cartService.removeItemsFromCart(userId, request.getSkuIds()), "Xóa các sản phẩm đã chọn thành công");
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xóa sạch toàn bộ giỏ hàng",
            description = "Xóa toàn bộ Redis key 'cart:user:{userId}' của người dùng"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Làm trống giỏ hàng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> clearCart(
            @RequestParam(required = false) Long customerId) {
        Long userId = securityUtils.getCurrentUserId(customerId);
        cartService.clearCart(userId);
        return ApiResponse.success(null, "Làm trống giỏ hàng thành công");
    }
}
