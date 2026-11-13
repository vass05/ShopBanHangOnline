package com.helishop.core.modules.order.controller;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.order.service.CheckoutService;
import com.helishop.core.modules.order.dto.CheckoutBatchRequest;
import com.helishop.core.modules.order.dto.CheckoutQuoteResponse;
import com.helishop.core.security.SecurityUtils;
import com.helishop.core.security.ResourceAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs đặt hàng, xử lý thanh toán và quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;
    private final ResourceAccess resourceAccess;
    private final CheckoutService checkoutService;

    @PostMapping("/quote")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Báo giá giỏ hàng nhiều shop", description = "Tính giá SKU, phí giao mỗi shop, điều kiện và phân bổ voucher; địa chỉ phải thuộc tài khoản hiện tại")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Báo giá thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Voucher hoặc dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ dành cho khách hàng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU hoặc địa chỉ không tồn tại"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không đủ tồn kho")
    })
    public ApiResponse<CheckoutQuoteResponse> quote(@Valid @RequestBody CheckoutBatchRequest request) {
        return ApiResponse.success(checkoutService.quote(securityUtils.getCurrentUserId(null), request));
    }

    @PostMapping("/checkout-batch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Đặt đơn nhiều shop", description = "Tính lại báo giá, khóa SKU, tạo một đơn mỗi shop trong cùng giao dịch. Idempotency-Key giữ nguyên khi thử lại cùng nội dung; giỏ được trừ sau commit.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đã tạo đơn hoặc trả lại đơn của yêu cầu trùng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Nội dung hoặc mã chống trùng không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ dành cho khách hàng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU hoặc địa chỉ không tồn tại"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không đủ tồn kho")
    })
    public ApiResponse<List<OrderResponse>> checkoutBatch(@RequestHeader("Idempotency-Key") String requestKey,
                                                         @Valid @RequestBody CheckoutBatchRequest request) {
        return ApiResponse.success(checkoutService.checkout(securityUtils.getCurrentUserId(null), request, requestKey), "Đặt hàng thành công");
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Tiến hành đặt hàng và trừ kho an toàn",
            description = "Tạo đơn cho khách hàng đang đăng nhập, kiểm tra customerId tương thích nếu được gửi, khóa PESSIMISTIC_WRITE trên từng SKU và trừ tồn kho"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đặt hàng thành công, tạo snapshot giá và trừ kho thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Thông tin đặt hàng không hợp lệ (Validation Error)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực tài khoản",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ khách hàng (ROLE_CUSTOMER) mới được quyền đặt hàng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Sản phẩm không đủ số lượng tồn kho (InsufficientStockException)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<OrderResponse> checkout(@RequestParam(required = false) Long customerId,
                                              @Valid @RequestBody CheckoutRequest request) {
        Long currentId = securityUtils.getCurrentUserId(customerId);
        securityUtils.getCurrentUserId(request.getCustomerId());
        return ApiResponse.success(orderService.checkout(currentId, request), "Đặt hàng thành công");
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xem chi tiết đơn hàng theo ID",
            description = "Tra cứu đơn theo ID, chỉ dành cho khách sở hữu đơn, người bán của shop hoặc quản trị viên"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy đơn hàng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng với ID yêu cầu",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        resourceAccess.requireOrder(id);
        return ApiResponse.success(orderService.getById(id));
    }

    @GetMapping("/code/{orderCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xem chi tiết đơn hàng theo mã Order Code",
            description = "Tra cứu theo mã đơn, chỉ dành cho khách sở hữu đơn, người bán của shop hoặc quản trị viên"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy đơn hàng",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng với Order Code",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<OrderResponse> getOrderByCode(@PathVariable String orderCode) {
        resourceAccess.requireOrderCode(orderCode);
        return ApiResponse.success(orderService.getByOrderCode(orderCode));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách đơn hàng theo khách hàng",
            description = "Truy xuất lịch sử mua hàng của chính khách hàng đang đăng nhập; quản trị viên được tra cứu khách hàng khác"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách đơn hàng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        resourceAccess.requireCustomer(customerId);
        return ApiResponse.success(orderService.getByCustomerId(customerId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật trạng thái đơn hàng",
            description = "Khách sở hữu chỉ được hủy đơn PENDING/CONFIRMED; người bán quản lý đơn của shop, quản trị viên quản lý toàn sàn. Hủy hoặc trả hàng sẽ hoàn kho."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái đơn hàng thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Trạng thái cập nhật không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng cần cập nhật",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                        @RequestParam OrderStatus status) {
        resourceAccess.requireOrderStatus(id, status);
        return ApiResponse.success(orderService.updateOrderStatus(id, status), "Cập nhật trạng thái đơn hàng thành công");
    }
}
