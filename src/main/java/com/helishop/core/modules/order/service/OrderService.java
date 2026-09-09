package com.helishop.core.modules.order.service;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.entity.OrderItem;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;

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
            ProductSku sku = productSkuRepository.findById(itemReq.getSkuId())
                    .orElseThrow(() -> new com.helishop.core.common.exception.ResourceNotFoundException("SKU", "id", itemReq.getSkuId()));

            if (sku.getStockQuantity() < itemReq.getQuantity()) {
                throw new com.helishop.core.common.exception.InsufficientStockException(sku.getSkuCode(), itemReq.getQuantity(), sku.getStockQuantity());
            }

            // Deduct stock with optimistic lock (@Version)
            sku.setStockQuantity(sku.getStockQuantity() - itemReq.getQuantity());
            productSkuRepository.save(sku);

            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productSku(sku)
                    .quantity(itemReq.getQuantity())
                    .priceAtPurchase(sku.getPrice())
                    .productNameSnapshot(sku.getProduct().getName())
                    .skuVariantSnapshot(sku.getSkuAttributes())
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Đơn hàng không tồn tại"));
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
                        .skuId(item.getProductSku().getId())
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
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
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
