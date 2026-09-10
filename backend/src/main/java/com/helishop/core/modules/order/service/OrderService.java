package com.helishop.core.modules.order.service;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.common.exception.ResourceNotFoundException;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.entity.OrderItem;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Tiến hành đặt hàng với Khóa bi quan (PESSIMISTIC_WRITE) chống âm kho
     */
    @Transactional
    public OrderResponse checkout(Long customerId, CheckoutRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();
        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(request.getPaymentMethod())
                .shippingAddressSnapshot(request.getShippingAddressSnapshot())
                .note(request.getNote())
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .build();

        for (CheckoutRequest.ItemRequest itemReq : request.getItems()) {
            // Khóa bi quan trên SKU: Các request đồng thời cạnh tranh sẽ xếp hàng chờ
            ProductSku sku = productSkuRepository.findByIdWithLock(itemReq.getSkuId())
                    .orElseThrow(() -> new ResourceNotFoundException("SKU", "id", itemReq.getSkuId()));

            if (sku.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(sku.getSkuCode(), itemReq.getQuantity(), sku.getStockQuantity());
            }

            // Trừ tồn kho SKU
            sku.setStockQuantity(sku.getStockQuantity() - itemReq.getQuantity());
            productSkuRepository.save(sku);

            // Đồng bộ trừ tồn kho Product (nếu có quản lý)
            if (sku.getProduct() != null) {
                Product product = productRepository.findByIdWithLock(sku.getProduct().getId()).orElse(null);
                if (product != null && product.getStockQuantity() != null && product.getStockQuantity() > 0) {
                    product.setStockQuantity(Math.max(0, product.getStockQuantity() - itemReq.getQuantity()));
                    productRepository.save(product);
                }
            }

            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productSku(sku)
                    .quantity(itemReq.getQuantity())
                    .priceAtPurchase(sku.getPrice())
                    .productNameSnapshot(sku.getProduct() != null ? sku.getProduct().getName() : sku.getSkuCode())
                    .skuVariantSnapshot(sku.getSkuAttributes())
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        log.info("Đặt hàng thành công orderCode='{}', tổng tiền={}", savedOrder.getOrderCode(), savedOrder.getFinalAmount());
        return mapToResponse(savedOrder);
    }

    /**
     * Alias method theo đặc tả Sprint 5: checkoutOrder
     */
    @Transactional
    public OrderResponse checkoutOrder(Long customerId, CheckoutRequest request) {
        return checkout(customerId, request);
    }

    /**
     * Cập nhật trạng thái đơn hàng. Nếu đơn bị HỦY hoặc TRẢ HÀNG thì tự động hoàn kho.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getOrderStatus() == newStatus) {
            return mapToResponse(order);
        }

        // Hoàn kho nếu chuyển sang trạng thái CANCELLED hoặc RETURNED từ trạng thái chưa hủy
        boolean isRollingBack = (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED)
                && order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getOrderStatus() != OrderStatus.RETURNED;

        if (isRollingBack) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductSku() != null) {
                    ProductSku sku = productSkuRepository.findByIdWithLock(item.getProductSku().getId()).orElse(null);
                    if (sku != null) {
                        sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity());
                        productSkuRepository.save(sku);

                        if (sku.getProduct() != null) {
                            Product product = productRepository.findByIdWithLock(sku.getProduct().getId()).orElse(null);
                            if (product != null && product.getStockQuantity() != null) {
                                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                                productRepository.save(product);
                            }
                        }
                    }
                }
            }
            log.info("Đã hoàn kho thành công cho đơn hàng #{}", order.getId());
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByOrderCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.ItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.ItemResponse.builder()
                        .id(item.getId())
                        .skuId(item.getProductSku() != null ? item.getProductSku().getId() : null)
                        .productNameSnapshot(item.getProductNameSnapshot())
                        .skuVariantSnapshot(item.getSkuVariantSnapshot())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : null)
                .shopId(order.getShop() != null ? order.getShop().getId() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .finalAmount(order.getFinalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddressSnapshot(order.getShippingAddressSnapshot())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
