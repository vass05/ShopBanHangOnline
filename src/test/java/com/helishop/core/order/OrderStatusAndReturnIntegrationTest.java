package com.helishop.core.order;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.entity.OrderItem;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusAndReturnIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductSkuRepository productSkuRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private Product sampleProduct;
    private ProductSku sampleSku;

    @BeforeEach
    void setUp() {
        User customer = User.builder().fullName("Khách hàng").build();
        customer.setId(1L);

        sampleProduct = Product.builder().name("iPhone 16").stockQuantity(8).build();
        sampleProduct.setId(100L);

        sampleSku = ProductSku.builder()
                .skuCode("IP16-128")
                .price(BigDecimal.valueOf(20000000))
                .stockQuantity(3) // Tồn kho hiện tại là 3
                .product(sampleProduct)
                .build();
        sampleSku.setId(10L);

        OrderItem item = OrderItem.builder()
                .productSku(sampleSku)
                .quantity(2) // Đơn hàng này đã mua 2 chiếc
                .priceAtPurchase(BigDecimal.valueOf(20000000))
                .subtotal(BigDecimal.valueOf(40000000))
                .productNameSnapshot("iPhone 16")
                .build();
        item.setId(1L);

        sampleOrder = Order.builder()
                .orderCode("ORD-RETURN-001")
                .customer(customer)
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Cầu Giấy, Hà Nội")
                .totalAmount(BigDecimal.valueOf(40000000))
                .finalAmount(BigDecimal.valueOf(40000000))
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        sampleOrder.setId(101L);
    }

    @Test
    @DisplayName("Hủy đơn hàng (CANCELLED): Tự động hoàn kho số lượng đã mua về lại SKU và Product")
    void shouldRollbackInventoryWhenOrderIsCancelled() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(sampleOrder));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(101L, OrderStatus.CANCELLED);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
        // Tồn kho ban đầu 3, hoàn trả 2 chiếc -> thành 5
        assertEquals(5, sampleSku.getStockQuantity());
        // Tồn kho product ban đầu 8, hoàn trả 2 chiếc -> thành 10
        assertEquals(10, sampleProduct.getStockQuantity());

        verify(productSkuRepository).save(sampleSku);
        verify(productRepository).save(sampleProduct);
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    @DisplayName("Đổi trả hàng (RETURNED): Tự động hoàn trả tồn kho đầy đủ")
    void shouldRollbackInventoryWhenOrderIsReturned() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(sampleOrder));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(101L, OrderStatus.RETURNED);

        assertNotNull(response);
        assertEquals(OrderStatus.RETURNED, response.getOrderStatus());
        assertEquals(5, sampleSku.getStockQuantity());
        assertEquals(10, sampleProduct.getStockQuantity());
    }

    @Test
    @DisplayName("Chuyển trạng thái giao hàng thông thường (SHIPPING): Không thay đổi tồn kho")
    void shouldNotChangeInventoryWhenNormalStatusTransition() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(101L, OrderStatus.SHIPPING);

        assertNotNull(response);
        assertEquals(OrderStatus.SHIPPING, response.getOrderStatus());
        // Tồn kho vẫn giữ nguyên 3
        assertEquals(3, sampleSku.getStockQuantity());
        assertEquals(8, sampleProduct.getStockQuantity());
    }
}
