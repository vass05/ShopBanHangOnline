package com.helishop.core.modules.order.service;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.security.ResourceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service @RequiredArgsConstructor
public class OrderLifecycleService {
    private final OrderRepository orders;
    private final OrderService orderService;
    private final ResourceAccess access;
    private final org.springframework.context.ApplicationEventPublisher events;

    @Transactional
    public OrderResponse update(Long id, OrderStatus target) {
        var order = orders.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
        // Recheck permissions and state after acquiring the lock, not only in the controller.
        access.requireOrderStatus(id, target);
        OrderStatus current = order.getOrderStatus();
        if (current == target) return orderService.mapToResponse(order);
        if (Set.of(OrderStatus.PROCESSING, OrderStatus.SHIPPING, OrderStatus.DELIVERED).contains(target)
                && order.getPaymentMethod() == com.helishop.core.common.constants.PaymentMethod.VNPAY
                && order.getPaymentStatus() != com.helishop.core.common.constants.PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn VNPAY cần được xác nhận thanh toán trước khi xử lý giao hàng");
        }
        Set<OrderStatus> allowed = switch (current) {
            case PENDING -> Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> Set.of(OrderStatus.PROCESSING, OrderStatus.SHIPPING, OrderStatus.CANCELLED);
            case PROCESSING -> Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED);
            case SHIPPING -> Set.of(OrderStatus.DELIVERED);
            case DELIVERED -> Set.of(OrderStatus.RETURNED);
            case CANCELLED, RETURNED -> Set.of();
        };
        if (!allowed.contains(target)) throw new AppException(ErrorCode.INVALID_REQUEST, "Không được chuyển trạng thái từ " + current + " sang " + target);
        OrderResponse result = orderService.updateOrderStatus(id, target);
        if (target == OrderStatus.CANCELLED || target == OrderStatus.RETURNED) {
            events.publishEvent(new com.helishop.core.modules.product.service.ProductStockChanged(order.getItems().stream()
                    .filter(item -> item.getProductSku() != null && item.getProductSku().getProduct() != null)
                    .map(item -> item.getProductSku().getProduct().getId()).collect(java.util.stream.Collectors.toSet())));
        }
        return result;
    }
}
