package com.helishop.core.modules.order.service;

import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class SellerOrderService {
    private final UserRepository users;
    private final ShopRepository shops;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final OrderLifecycleService lifecycle;
    private final SellerOrderPolicy policy;

    private Shop shop(Long userId) {
        var user = users.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getRole() != UserRole.ROLE_SELLER || user.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED);
        Shop shop = shops.findByOwnerId(userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bạn cần mở gian hàng trước"));
        policy.requireActiveShop(shop);
        return shop;
    }

    public PageResponse<OrderResponse> list(Long userId, int page, int size, String keyword, OrderStatus status, PaymentStatus paymentStatus) {
        Shop shop = shop(userId);
        if (page < 1 || size < 1 || size > 50 || (keyword != null && keyword.length() > 64)) throw new AppException(ErrorCode.INVALID_REQUEST, "Bộ lọc đơn hàng không hợp lệ");
        Specification<Order> spec = (root, query, cb) -> cb.equal(root.get("shop").get("id"), shop.getId());
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("orderStatus"), status));
        if (paymentStatus != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), paymentStatus));
        if (keyword != null && !keyword.isBlank()) {
            String code = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("orderCode")), code));
        }
        var result = orders.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        // Do not fetch item collections in a paginated query; detail loads the snapshots.
        return PageResponse.from(result.map(order -> OrderResponse.builder().id(order.getId()).orderCode(order.getOrderCode())
                .customerId(order.getCustomer().getId()).customerName(order.getCustomer().getFullName()).shopId(shop.getId()).shopName(shop.getShopName())
                .orderStatus(order.getOrderStatus()).paymentMethod(order.getPaymentMethod()).paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount()).shippingFee(order.getShippingFee()).discountAmount(order.getDiscountAmount()).finalAmount(order.getFinalAmount())
                .paymentExpiresAt(order.getPaymentExpiresAt()).createdAt(order.getCreatedAt()).items(List.of()).build()));
    }

    public SellerOrderResponse detail(Long userId, Long id) {
        Shop shop = shop(userId);
        Order order = orders.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
        own(order, shop);
        return response(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SellerOrderResponse update(Long userId, Long id, SellerOrderStatusRequest request) {
        Shop shop = shop(userId);
        Order order = orders.findByIdWithLock(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
        own(order, shop);
        if (order.getOrderStatus() != request.status() && order.getOrderStatus() != request.expectedStatus()) {
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "Đơn đã thay đổi ở nơi khác. Hãy tải lại dữ liệu trước khi xử lý");
        }
        policy.requireTransition(order, request.status());
        lifecycle.update(id, request.status());
        orders.flush();
        return response(order);
    }

    private void own(Order order, Shop shop) {
        if (order.getShop() == null || !shop.getId().equals(order.getShop().getId())) throw new AppException(ErrorCode.UNAUTHORIZED, "Đơn hàng không thuộc gian hàng của bạn");
    }
    private SellerOrderResponse response(Order order) { return new SellerOrderResponse(orderService.mapToResponse(order), policy.allowed(order), policy.note(order), order.getUpdatedAt()); }
}
