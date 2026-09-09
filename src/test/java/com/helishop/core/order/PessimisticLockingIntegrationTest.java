package com.helishop.core.order;

import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessimisticLockingIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductSkuRepository productSkuRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User sampleCustomer;
    private Product sampleProduct;
    private ProductSku sampleSku;

    @BeforeEach
    void setUp() {
        sampleCustomer = User.builder().fullName("Khách hàng Test").build();
        sampleCustomer.setId(1L);

        sampleProduct = Product.builder().name("iPhone 16 Flash Sale").stockQuantity(10).build();
        sampleProduct.setId(100L);

        sampleSku = ProductSku.builder()
                .skuCode("IP16-FLASH-128")
                .price(BigDecimal.valueOf(20000000))
                .stockQuantity(5) // Hiện chỉ còn 5 chiếc
                .product(sampleProduct)
                .build();
        sampleSku.setId(10L);
    }

    @Test
    @DisplayName("Khóa bi quan: Đặt hàng thành công khi số lượng tồn kho đáp ứng đủ, trừ kho chính xác")
    void shouldCheckoutSuccessfullyWhenStockIsSufficient() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        // Khóa bi quan findByIdWithLock
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(500L);
            return order;
        });

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Phố Huế, Hà Nội")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(2).build()))
                .build();

        OrderResponse response = orderService.checkout(1L, request);

        assertNotNull(response);
        // Tồn kho ban đầu 5, mua 2 -> còn 3
        assertEquals(3, sampleSku.getStockQuantity());
        assertEquals(8, sampleProduct.getStockQuantity());
        verify(productSkuRepository).save(sampleSku);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Khóa bi quan: Ném InsufficientStockException (HTTP 409) ngay lập tức khi kho không đủ số lượng yêu cầu")
    void shouldThrowInsufficientStockExceptionWhenRequestedQuantityExceedsStock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku)); // tồn kho 5

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("123 Phố Huế, Hà Nội")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(10).build())) // đòi mua 10
                .build();

        assertThrows(InsufficientStockException.class, () -> orderService.checkout(1L, request));
        // Số lượng tồn kho vẫn phải giữ nguyên 5
        assertEquals(5, sampleSku.getStockQuantity());
    }
}
