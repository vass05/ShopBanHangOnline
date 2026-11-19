package com.helishop.core.modules.order.service;

import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.user.entity.Shop;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class SellerOrderPolicy {
    public void requireActiveShop(Shop shop) {
        if (shop == null || shop.getStatus() != ShopStatus.ACTIVE || shop.getOwner() == null
                || shop.getOwner().getRole() != UserRole.ROLE_SELLER || shop.getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản người bán và gian hàng phải đang hoạt động");
        }
    }

    public List<OrderStatus> allowed(Order order) {
        List<OrderStatus> result = new ArrayList<>();
        OrderStatus next = switch (order.getOrderStatus()) {
            case PENDING -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.PROCESSING;
            case PROCESSING -> OrderStatus.SHIPPING;
            case SHIPPING -> OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED, RETURNED -> null;
        };
        boolean refunded = order.getPaymentStatus() == PaymentStatus.REFUNDED;
        boolean awaitingOnlinePayment = order.getPaymentMethod() == PaymentMethod.VNPAY && order.getPaymentStatus() != PaymentStatus.PAID;
        if (next != null && !refunded && (next == OrderStatus.CONFIRMED || !awaitingOnlinePayment)) result.add(next);
        boolean unpaid = order.getPaymentStatus() == PaymentStatus.UNPAID || order.getPaymentStatus() == PaymentStatus.FAILED
                || (order.getPaymentStatus() == PaymentStatus.PENDING && order.getPaymentExpiresAt() != null && !order.getPaymentExpiresAt().isAfter(LocalDateTime.now()));
        if (unpaid && Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING).contains(order.getOrderStatus())) result.add(OrderStatus.CANCELLED);
        return List.copyOf(result);
    }

    public void requireTransition(Order order, OrderStatus target) {
        requireActiveShop(order.getShop());
        if (target == order.getOrderStatus()) return;
        if (!allowed(order).contains(target)) throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể chuyển trạng thái này. Kiểm tra bước xử lý và tình trạng thanh toán của đơn");
    }

    public String note(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID && Set.of(OrderStatus.CANCELLED, OrderStatus.RETURNED).contains(order.getOrderStatus())) return "Tiền đã được ghi nhận cho đơn đã đóng; cần đối soát/hoàn tiền riêng, không tự mở lại đơn";
        if (Set.of(OrderStatus.CANCELLED, OrderStatus.RETURNED).contains(order.getOrderStatus())) return "Đơn đã kết thúc xử lý; không thể chuyển sang bước giao hàng";
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) return "Đơn đã hoàn tiền; không tiếp tục xử lý giao hàng";
        if (order.getPaymentMethod() == PaymentMethod.VNPAY && order.getPaymentStatus() != PaymentStatus.PAID) return "Đơn VNPAY chưa được xác nhận thanh toán; chưa thể đóng gói hoặc giao hàng";
        if (order.getOrderStatus() == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.PAID) return "Đã ghi nhận giao hàng; thanh toán COD chưa được đối soát";
        if (order.getOrderStatus() == OrderStatus.SHIPPING) return "Đánh dấu đã giao là ghi nhận thủ công, chưa kết nối hãng vận chuyển";
        if (order.getPaymentStatus() == PaymentStatus.PAID && Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING).contains(order.getOrderStatus())) return "Đơn đã thanh toán; chức năng này không hủy hoặc hoàn tiền";
        return null;
    }
}
