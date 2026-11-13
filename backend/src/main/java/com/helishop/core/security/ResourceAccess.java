package com.helishop.core.security;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAccess {
    private final SecurityUtils securityUtils;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public void requireCustomer(Long customerId) {
        if (!securityUtils.hasRole("ADMIN")) {
            securityUtils.getCurrentUserId(customerId);
        }
    }

    public void requireOrder(Long id) {
        requireOrderAccess(findOrder(id));
    }

    public void requireOrderCode(String code) {
        requireOrderAccess(orderRepository.findByOrderCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại")));
    }

    public void requireOrderStatus(Long id, OrderStatus status) {
        Order order = findOrder(id);
        requireOrderAccess(order);
        if (!securityUtils.hasRole("ADMIN") && !ownsShop(order.getShop())) {
            if (status == OrderStatus.CANCELLED && order.getOrderStatus() == OrderStatus.CANCELLED) return;
            if (status != OrderStatus.CANCELLED
                    || order.getPaymentStatus() == com.helishop.core.common.constants.PaymentStatus.PAID
                    || (order.getPaymentStatus() == com.helishop.core.common.constants.PaymentStatus.PENDING
                        && (order.getPaymentExpiresAt() == null || order.getPaymentExpiresAt().isAfter(java.time.LocalDateTime.now())))
                    || (order.getOrderStatus() != OrderStatus.PENDING
                    && order.getOrderStatus() != OrderStatus.CONFIRMED)) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Khách hàng chỉ được hủy đơn chưa xử lý và chưa thanh toán");
            }
        }
    }

    public void requireShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop không tồn tại"));
        if (!securityUtils.hasRole("ADMIN") && !ownsShop(shop)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền quản lý gian hàng này");
        }
    }

    public void requireProduct(Long productId, Long requestedShopId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        requireShop(product.getShop().getId());
        if (requestedShopId != null && !requestedShopId.equals(product.getShop().getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không được chuyển sản phẩm sang gian hàng khác");
        }
    }

    public void requireVisibleProduct(com.helishop.core.modules.product.dto.ProductResponse product) {
        if (product != null && product.getStatus() == com.helishop.core.common.constants.ProductStatus.ACTIVE
                && product.getShopStatus() != com.helishop.core.common.constants.ShopStatus.INACTIVE
                && product.getShopStatus() != com.helishop.core.common.constants.ShopStatus.BANNED) return;
        if (securityUtils.hasRole("ADMIN")) return;
        if (product != null && securityUtils.hasRole("SELLER")
                && shopRepository.findById(product.getShopId()).map(this::ownsShop).orElse(false)) return;
        throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
    }

    private void requireOrderAccess(Order order) {
        if (securityUtils.hasRole("ADMIN") || ownsShop(order.getShop())) {
            return;
        }
        Long userId = securityUtils.getCurrentUserId(null);
        if (order.getCustomer() == null || !userId.equals(order.getCustomer().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập đơn hàng này");
        }
    }

    private boolean ownsShop(Shop shop) {
        return securityUtils.hasRole("SELLER") && shop != null && shop.getOwner() != null
                && securityUtils.getCurrentUserId(null).equals(shop.getOwner().getId());
    }
}
