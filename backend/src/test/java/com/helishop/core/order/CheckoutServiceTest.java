package com.helishop.core.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.entity.*;
import com.helishop.core.modules.order.repository.*;
import com.helishop.core.modules.order.service.CheckoutService;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.entity.*;
import com.helishop.core.modules.product.repository.*;
import com.helishop.core.modules.user.entity.*;
import com.helishop.core.modules.user.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {
    UserRepository users;
    UserAddressRepository addresses;
    ProductSkuRepository skus;
    ProductRepository products;
    VoucherRepository vouchers;
    OrderRepository orders;
    CheckoutReceiptRepository receipts;
    RedisCartService cart;
    CheckoutService service;
    Map<Long, ProductSku> inventory;
    Map<Long, Order> saved;
    Map<String, CheckoutReceipt> attempts;
    Voucher voucher;

    @BeforeEach void setup() {
        users = mock(UserRepository.class); addresses = mock(UserAddressRepository.class);
        skus = mock(ProductSkuRepository.class); products = mock(ProductRepository.class);
        vouchers = mock(VoucherRepository.class); orders = mock(OrderRepository.class);
        receipts = mock(CheckoutReceiptRepository.class); cart = mock(RedisCartService.class);
        service = new CheckoutService(users, addresses, skus, products, vouchers, orders, receipts,
                new OrderService(orders, skus, products, users), cart, new ObjectMapper());
        inventory = new TreeMap<>(); saved = new HashMap<>(); attempts = new HashMap<>();
        User customer = User.builder().status(UserStatus.ACTIVE).build(); customer.setId(1L);
        when(users.findByIdWithLock(1L)).thenReturn(Optional.of(customer));
        when(addresses.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(UserAddress.builder().recipientName("Buyer")
                .phone("0900000000").streetAddress("Street").ward("Ward").district("District").provinceCity("City").build()));
        for (long id = 10; id <= 20; id += 10) {
            Shop shop = Shop.builder().shopName("Shop " + id).status(ShopStatus.ACTIVE).build(); shop.setId(id);
            Product product = Product.builder().name("Product " + id).shop(shop).status(ProductStatus.ACTIVE).stockQuantity(10).build(); product.setId(id);
            ProductSku sku = ProductSku.builder().product(product).skuCode("SKU" + id).price(BigDecimal.valueOf(id * 10000)).stockQuantity(10).build(); sku.setId(id);
            inventory.put(id, sku);
            when(skus.findByIdWithProductAndShop(id)).thenReturn(Optional.of(sku));
            when(skus.findByIdWithLock(id)).thenReturn(Optional.of(sku));
            when(products.findByIdWithLock(id)).thenReturn(Optional.of(product));
        }
        when(orders.save(any())).thenAnswer(invocation -> { Order order = invocation.getArgument(0); order.setId((long) saved.size() + 1); saved.put(order.getId(), order); return order; });
        when(orders.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(saved.get(invocation.getArgument(0))));
        when(receipts.findByCustomerIdAndRequestKey(eq(1L), anyString())).thenAnswer(invocation -> Optional.ofNullable(attempts.get(invocation.getArgument(1))));
        when(receipts.save(any())).thenAnswer(invocation -> { CheckoutReceipt receipt = invocation.getArgument(0); attempts.put(receipt.getRequestKey(), receipt); return receipt; });
        voucher = Voucher.builder().voucherCode("HELI50K").status(VoucherStatus.ACTIVE).discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000)).minOrderValue(BigDecimal.valueOf(200000)).usedCount(0).usageLimit(5)
                .startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(1)).build();
        when(vouchers.findByVoucherCode("HELI50K")).thenReturn(Optional.of(voucher));
        when(vouchers.findByCodeWithLock("HELI50K")).thenReturn(Optional.of(voucher));
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }
    CheckoutBatchRequest request() {
        return CheckoutBatchRequest.builder().addressId(5L).shippingMethod("standard").paymentMethod(PaymentMethod.COD).voucherCode("HELI50K")
                .items(List.of(CheckoutRequest.ItemRequest.builder().skuId(20L).quantity(1).build(), CheckoutRequest.ItemRequest.builder().skuId(10L).quantity(1).build())).build();
    }

    @Test void quoteUsesServerPricesShippingAndAllocatesVoucherExactly() {
        var quote = service.quote(1L, request());
        assertEquals(0, quote.totalAmount().compareTo(BigDecimal.valueOf(300000)));
        assertEquals(0, quote.shippingFee().compareTo(BigDecimal.valueOf(60000)));
        assertEquals(0, quote.finalAmount().compareTo(BigDecimal.valueOf(310000)));
        assertEquals(0, quote.shops().stream().map(CheckoutQuoteResponse.ShopQuote::discountAmount).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.valueOf(50000)));
        verify(orders, never()).save(any());
        assertEquals(10, inventory.get(10L).getStockQuantity());
    }

    @Test void checkoutCreatesOneOrderPerShopAndRepeatedKeyDoesNotDeductTwice() {
        var first = service.checkout(1L, request(), "checkout-request-0001");
        var repeated = service.checkout(1L, request(), "checkout-request-0001");
        assertEquals(2, first.size());
        assertEquals(first.stream().map(OrderResponse::getId).toList(), repeated.stream().map(OrderResponse::getId).toList());
        assertEquals(Set.of(10L, 20L), new HashSet<>(first.stream().map(OrderResponse::getShopId).toList()));
        assertEquals(9, inventory.get(10L).getStockQuantity());
        assertEquals(9, inventory.get(20L).getStockQuantity());
        assertEquals(1, voucher.getUsedCount());
        assertEquals(2, saved.size());
        var orderedLocks = inOrder(skus);
        orderedLocks.verify(skus).findByIdWithLock(10L);
        orderedLocks.verify(skus).findByIdWithLock(20L);
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
        verify(cart).consumePurchasedItems(1L, Map.of(10L, 1, 20L, 1));
    }

    @Test void rejectsReusingKeyWithDifferentContent() {
        service.checkout(1L, request(), "checkout-request-0001");
        var changed = request(); changed.setShippingMethod("express");
        assertThrows(AppException.class, () -> service.checkout(1L, changed, "checkout-request-0001"));
        assertEquals(2, saved.size());
    }

    @Test void insufficientStockDoesNotCreateAnyOrderOrMutateOtherSkus() {
        inventory.get(20L).setStockQuantity(0);
        assertThrows(AppException.class, () -> service.checkout(1L, request(), "checkout-request-0001"));
        assertEquals(10, inventory.get(10L).getStockQuantity());
        verify(orders, never()).save(any());
        verify(receipts, never()).save(any());
    }

    @Test void addressMustBelongToCurrentCustomer() {
        var request = request(); request.setAddressId(99L);
        assertThrows(AppException.class, () -> service.quote(1L, request));
        verify(orders, never()).save(any());
    }

    @Test void expiredVoucherAndNonpositiveQuantitiesAreRejected() {
        voucher.setEndDate(LocalDateTime.now().minusMinutes(1));
        assertThrows(AppException.class, () -> service.quote(1L, request()));
        var request = request(); request.getItems().getFirst().setQuantity(-1);
        assertThrows(AppException.class, () -> service.quote(1L, request));
    }

    @Test void shopVoucherOnlyReducesEligibleShopAndFreeshipNeverExceedsShipping() {
        voucher.setMinOrderValue(BigDecimal.ZERO);
        voucher.setShop(inventory.get(10L).getProduct().getShop());
        var quote = service.quote(1L, request());
        assertEquals(0, quote.shops().get(1).discountAmount().signum());
        assertEquals(0, quote.shops().getFirst().discountAmount().compareTo(BigDecimal.valueOf(50000)));
        voucher.setVoucherCode("FREESHIP");
        quote = service.quote(1L, request());
        assertEquals(0, quote.discountAmount().compareTo(BigDecimal.valueOf(30000)));
    }
}
