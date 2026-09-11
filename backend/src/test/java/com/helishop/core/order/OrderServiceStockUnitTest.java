package com.helishop.core.order;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.common.exception.InsufficientStockException;
import com.helishop.core.common.exception.ResourceNotFoundException;
import com.helishop.core.modules.order.dto.CheckoutRequest;
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
import com.helishop.core.modules.user.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 6: OrderService Stock & Exception Unit Tests")
class OrderServiceStockUnitTest {

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
        sampleCustomer = User.builder()
                .fullName("Khách hàng Test")
                .email("customer@test.com")
                .build();
        sampleCustomer.setId(1L);

        sampleProduct = Product.builder()
                .name("MacBook Pro M4")
                .stockQuantity(20)
                .build();
        sampleProduct.setId(100L);

        sampleSku = ProductSku.builder()
                .skuCode("MBP-M4-16-512")
                .price(BigDecimal.valueOf(45000000))
                .stockQuantity(10)
                .product(sampleProduct)
                .skuAttributes("{\"ram\":\"16GB\",\"ssd\":\"512GB\"}")
                .build();
        sampleSku.setId(10L);
    }

    @Test
    @DisplayName("Unit Test 1: Đặt hàng thành công khi kho đủ hàng -> Trừ kho SKU và Product chính xác")
    void shouldDeductStockSuccessfully_WhenStockIsSufficient() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(888L);
            return order;
        });

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội, Việt Nam")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(3).build()))
                .build();

        OrderResponse response = orderService.checkout(1L, request);

        assertNotNull(response);
        assertEquals(888L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
        assertEquals(BigDecimal.valueOf(135000000), response.getTotalAmount()); // 45tr * 3

        // Kiểm tra tồn kho bị trừ
        assertEquals(7, sampleSku.getStockQuantity());     // 10 - 3 = 7
        assertEquals(17, sampleProduct.getStockQuantity()); // 20 - 3 = 17

        verify(productSkuRepository).save(sampleSku);
        verify(productRepository).save(sampleProduct);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 2: Ném ngoại lệ InsufficientStockException khi yêu cầu số lượng vượt tồn kho")
    void shouldThrowInsufficientStockException_WhenRequestedQuantityExceedsSkuStock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku)); // Hiện chỉ có 10

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội, Việt Nam")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(15).build())) // Đòi mua 15
                .build();

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.checkout(1L, request)
        );

        // Kiểm tra thông tin trong ngoại lệ
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("MBP-M4-16-512"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("15"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("10"));

        // Đảm bảo số lượng kho không bị trừ khi phát sinh lỗi
        assertEquals(10, sampleSku.getStockQuantity());
        assertEquals(20, sampleProduct.getStockQuantity());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 3: Ném ngoại lệ ResourceNotFoundException khi SKU không tồn tại")
    void shouldThrowResourceNotFoundException_WhenSkuNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productSkuRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội, Việt Nam")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(999L).quantity(1).build()))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> orderService.checkout(1L, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 4: Ném ngoại lệ AppException(USER_NOT_EXISTED) khi khách hàng không tồn tại")
    void shouldThrowAppException_WhenCustomerNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CheckoutRequest request = CheckoutRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .shippingAddressSnapshot("Hà Nội, Việt Nam")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(1).build()))
                .build();

        AppException ex = assertThrows(AppException.class, () -> orderService.checkout(999L, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        verify(productSkuRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Unit Test 5: Tự động hoàn trả tồn kho khi trạng thái đơn hàng chuyển sang CANCELLED")
    void shouldRestoreStock_WhenOrderStatusChangedToCancelled() {
        OrderItem item = OrderItem.builder()
                .productSku(sampleSku)
                .quantity(4)
                .priceAtPurchase(sampleSku.getPrice())
                .subtotal(sampleSku.getPrice().multiply(BigDecimal.valueOf(4)))
                .build();

        Order existingOrder = Order.builder()
                .orderStatus(OrderStatus.PENDING)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        existingOrder.setId(777L);

        sampleSku.setStockQuantity(6);      // giả sử kho còn 6
        sampleProduct.setStockQuantity(16); // product còn 16

        when(orderRepository.findById(777L)).thenReturn(Optional.of(existingOrder));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(777L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
        // Hoàn kho +4
        assertEquals(10, sampleSku.getStockQuantity());
        assertEquals(20, sampleProduct.getStockQuantity());

        verify(productSkuRepository).save(sampleSku);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Unit Test 6: Tự động hoàn trả tồn kho khi trạng thái đơn hàng chuyển sang RETURNED")
    void shouldRestoreStock_WhenOrderStatusChangedToReturned() {
        OrderItem item = OrderItem.builder()
                .productSku(sampleSku)
                .quantity(2)
                .priceAtPurchase(sampleSku.getPrice())
                .subtotal(sampleSku.getPrice().multiply(BigDecimal.valueOf(2)))
                .build();

        Order existingOrder = Order.builder()
                .orderStatus(OrderStatus.DELIVERED)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        existingOrder.setId(778L);

        sampleSku.setStockQuantity(8);
        sampleProduct.setStockQuantity(18);

        when(orderRepository.findById(778L)).thenReturn(Optional.of(existingOrder));
        when(productSkuRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleSku));
        when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(778L, OrderStatus.RETURNED);

        assertEquals(OrderStatus.RETURNED, response.getOrderStatus());
        // Hoàn kho +2
        assertEquals(10, sampleSku.getStockQuantity());
        assertEquals(20, sampleProduct.getStockQuantity());

        verify(productSkuRepository).save(sampleSku);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Unit Test 7: Không hoàn kho lần 2 nếu đơn hàng đã ở trạng thái CANCELLED trước đó")
    void shouldNotDoubleRestoreStock_WhenOrderAlreadyCancelled() {
        Order cancelledOrder = Order.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .orderItems(new ArrayList<>())
                .build();
        cancelledOrder.setId(779L);

        when(orderRepository.findById(779L)).thenReturn(Optional.of(cancelledOrder));

        orderService.updateOrderStatus(779L, OrderStatus.CANCELLED);

        // Không gọi findByIdWithLock hay save thêm lần nào nữa
        verify(productSkuRepository, never()).findByIdWithLock(any());
        verify(productRepository, never()).findByIdWithLock(any());
    }
}
