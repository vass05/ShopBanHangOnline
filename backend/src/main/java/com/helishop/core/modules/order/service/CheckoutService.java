package com.helishop.core.modules.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.entity.*;
import com.helishop.core.modules.order.repository.*;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.UserAddress;
import com.helishop.core.modules.user.repository.UserAddressRepository;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class CheckoutService {
    private final UserRepository users;
    private final UserAddressRepository addresses;
    private final ProductSkuRepository skus;
    private final ProductRepository products;
    private final VoucherRepository vouchers;
    private final OrderRepository orders;
    private final CheckoutReceiptRepository receipts;
    private final OrderService orderService;
    private final RedisCartService cart;
    private final ObjectMapper json;

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse quote(Long userId, CheckoutBatchRequest request) {
        address(userId, request.getAddressId());
        return price(request, load(request, false), voucher(request, false));
    }

    @Transactional
    public List<OrderResponse> checkout(Long userId, CheckoutBatchRequest request, String requestKey) {
        if (requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{16,80}")) throw invalid("Mã chống đặt trùng không hợp lệ");
        var customer = users.findByIdWithLock(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (customer.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED);
        String fingerprint = fingerprint(request);
        var previous = receipts.findByCustomerIdAndRequestKey(userId, requestKey);
        if (previous.isPresent()) {
            if (!previous.get().getFingerprint().equals(fingerprint)) throw invalid("Mã đặt hàng đã được dùng với nội dung khác");
            return restore(previous.get());
        }

        UserAddress address = address(userId, request.getAddressId());
        Voucher voucher = voucher(request, true);
        // All SKU rows are locked in ascending ID order before any product row.
        Map<Long, ProductSku> locked = load(request, true);
        Map<Long, com.helishop.core.modules.product.entity.Product> lockedProducts = new TreeMap<>();
        locked.values().stream().map(sku -> sku.getProduct().getId()).distinct().sorted().forEach(productId ->
                lockedProducts.put(productId, products.findByIdWithLock(productId)
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND))));
        CheckoutQuoteResponse quote = price(request, locked, voucher);
        String snapshot = String.join(", ", address.getRecipientName() + " - " + address.getPhone(),
                address.getStreetAddress(), address.getWard(), address.getDistrict(), address.getProvinceCity());
        List<OrderResponse> result = new ArrayList<>();
        for (var group : quote.shops()) {
            var order = Order.builder().orderCode("ORD-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT))
                    .customer(customer).shop(locked.get(group.items().getFirst().skuId()).getProduct().getShop())
                    .orderStatus(OrderStatus.PENDING).paymentStatus(PaymentStatus.UNPAID)
                    .paymentMethod(request.getPaymentMethod()).shippingAddressSnapshot(snapshot).note(request.getNote())
                    .totalAmount(group.totalAmount()).shippingFee(group.shippingFee())
                    .discountAmount(group.discountAmount()).finalAmount(group.finalAmount()).build();
            for (var item : group.items()) {
                ProductSku sku = locked.get(item.skuId());
                sku.setStockQuantity(sku.getStockQuantity() - item.quantity());
                var product = lockedProducts.get(sku.getProduct().getId());
                if (product.getStockQuantity() != null) product.setStockQuantity(Math.max(0, product.getStockQuantity() - item.quantity()));
                order.getItems().add(OrderItem.builder().order(order).productSku(sku).quantity(item.quantity())
                        .productNameSnapshot(item.productName()).skuVariantSnapshot(item.skuVariant())
                        .priceAtPurchase(item.unitPrice()).subtotal(item.subtotal()).build());
            }
            result.add(orderService.mapToResponse(orders.save(order)));
        }
        if (voucher != null) voucher.setUsedCount(voucher.getUsedCount() + 1);
        try {
            receipts.save(CheckoutReceipt.builder().customerId(userId).requestKey(requestKey).fingerprint(fingerprint)
                    .orderIds(json.writeValueAsString(result.stream().map(OrderResponse::getId).toList())).build());
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể ghi nhận phiên đặt hàng");
        }
        Map<Long, Integer> purchased = quantities(request);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { cart.consumePurchasedItems(userId, purchased); }
                catch (RuntimeException exception) { log.warn("Đơn đã lưu nhưng chưa dọn được giỏ cho userId={}", userId); }
            }
        });
        return result;
    }

    private UserAddress address(Long userId, Long addressId) {
        if (addressId == null) throw invalid("Vui lòng chọn địa chỉ nhận hàng");
        return addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Địa chỉ không thuộc tài khoản hiện tại"));
    }

    private Map<Long, Integer> quantities(CheckoutBatchRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty() || request.getItems().size() > 100) throw invalid("Danh sách mua không hợp lệ");
        Map<Long, Integer> quantities = new TreeMap<>();
        for (var item : request.getItems()) {
            if (item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) throw invalid("Số lượng mua phải lớn hơn 0");
            try { quantities.merge(item.getSkuId(), item.getQuantity(), Math::addExact); }
            catch (ArithmeticException exception) { throw invalid("Số lượng mua quá lớn"); }
        }
        return quantities;
    }

    private Map<Long, ProductSku> load(CheckoutBatchRequest request, boolean lock) {
        Map<Long, ProductSku> loaded = new TreeMap<>();
        for (Long skuId : quantities(request).keySet()) {
            loaded.put(skuId, (lock ? skus.findByIdWithLock(skuId) : skus.findByIdWithProductAndShop(skuId))
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "SKU không còn tồn tại")));
        }
        return loaded;
    }

    private Voucher voucher(CheckoutBatchRequest request, boolean lock) {
        if (request.getVoucherCode() == null || request.getVoucherCode().isBlank()) return null;
        String code = request.getVoucherCode().trim().toUpperCase(Locale.ROOT);
        return (lock ? vouchers.findByCodeWithLock(code) : vouchers.findByVoucherCode(code))
                .orElseThrow(() -> invalid("Voucher không tồn tại"));
    }

    private CheckoutQuoteResponse price(CheckoutBatchRequest request, Map<Long, ProductSku> loaded, Voucher voucher) {
        if (request.getPaymentMethod() != PaymentMethod.COD && request.getPaymentMethod() != PaymentMethod.VNPAY) throw invalid("Phương thức thanh toán chưa được hỗ trợ");
        BigDecimal fee = switch (Objects.toString(request.getShippingMethod(), "")) {
            case "standard" -> BigDecimal.valueOf(30000);
            case "express" -> BigDecimal.valueOf(50000);
            case "economy" -> BigDecimal.valueOf(15000);
            default -> throw invalid("Phương thức giao hàng không hợp lệ");
        };
        Map<Long, List<CheckoutQuoteResponse.Item>> groups = new TreeMap<>();
        Map<Long, String> names = new HashMap<>();
        for (var entry : quantities(request).entrySet()) {
            ProductSku sku = loaded.get(entry.getKey());
            var product = sku.getProduct();
            if (product == null || product.getShop() == null || product.getStatus() != ProductStatus.ACTIVE
                    || product.getShop().getStatus() != ShopStatus.ACTIVE) throw invalid("Sản phẩm hoặc gian hàng ngừng bán");
            if (sku.getStockQuantity() < entry.getValue()) throw new AppException(ErrorCode.INSUFFICIENT_STOCK, "Sản phẩm không đủ tồn kho: " + product.getName());
            if (sku.getPrice() == null || sku.getPrice().signum() < 0) throw invalid("Giá SKU không hợp lệ");
            Long shopId = product.getShop().getId();
            names.put(shopId, product.getShop().getShopName());
            groups.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(new CheckoutQuoteResponse.Item(sku.getId(), product.getId(),
                    product.getName(), sku.getSkuCode(), sku.getSkuImageUrl() != null ? sku.getSkuImageUrl() : product.getMainImageUrl(),
                    sku.getSkuAttributes(), entry.getValue(), sku.getPrice(), sku.getPrice().multiply(BigDecimal.valueOf(entry.getValue()))));
        }
        Map<Long, BigDecimal> totals = new TreeMap<>();
        groups.forEach((shopId, items) -> totals.put(shopId, items.stream().map(CheckoutQuoteResponse.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)));
        NavigableSet<Long> eligible = new TreeSet<>(groups.keySet());
        if (voucher != null && voucher.getShop() != null) eligible.retainAll(Set.of(voucher.getShop().getId()));
        BigDecimal eligibleTotal = eligible.stream().map(totals::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean freeship = voucher != null && "FREESHIP".equals(voucher.getVoucherCode());
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher != null) {
            LocalDateTime now = LocalDateTime.now();
            if (voucher.getStatus() != VoucherStatus.ACTIVE || now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())
                    || (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit())
                    || eligible.isEmpty() || eligibleTotal.compareTo(Objects.requireNonNullElse(voucher.getMinOrderValue(), BigDecimal.ZERO)) < 0) throw invalid("Voucher hết hạn, hết lượt hoặc chưa đủ điều kiện áp dụng");
            discount = voucher.getDiscountType() == DiscountType.PERCENTAGE
                    ? eligibleTotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN) : voucher.getDiscountValue();
            if (voucher.getMaxDiscountAmount() != null) discount = discount.min(voucher.getMaxDiscountAmount());
            discount = discount.max(BigDecimal.ZERO).min(freeship ? fee.multiply(BigDecimal.valueOf(eligible.size())) : eligibleTotal);
        }
        BigDecimal remaining = discount;
        List<CheckoutQuoteResponse.ShopQuote> quotes = new ArrayList<>();
        for (Long shopId : groups.keySet()) {
            BigDecimal share = BigDecimal.ZERO;
            if (eligible.contains(shopId) && discount.signum() > 0) {
                BigDecimal weight = freeship ? BigDecimal.ONE : totals.get(shopId);
                BigDecimal denominator = freeship ? BigDecimal.valueOf(eligible.size()) : eligibleTotal;
                share = shopId.equals(eligible.last()) ? remaining
                        : discount.multiply(weight).divide(denominator, 0, RoundingMode.DOWN);
                remaining = remaining.subtract(share);
            }
            BigDecimal total = totals.get(shopId);
            quotes.add(new CheckoutQuoteResponse.ShopQuote(shopId, names.get(shopId), total, fee, share, total.add(fee).subtract(share), groups.get(shopId)));
        }
        BigDecimal total = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = fee.multiply(BigDecimal.valueOf(groups.size()));
        return new CheckoutQuoteResponse(total, shipping, discount, total.add(shipping).subtract(discount), quotes);
    }

    private String fingerprint(CheckoutBatchRequest request) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request))); }
        catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể xác minh phiên đặt hàng");
        }
    }

    private List<OrderResponse> restore(CheckoutReceipt receipt) {
        try {
            List<Long> ids = json.readValue(receipt.getOrderIds(), new TypeReference<>() {});
            return ids.stream().map(orderService::getById).toList();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không đọc được phiên đặt hàng");
        }
    }

    private AppException invalid(String message) { return new AppException(ErrorCode.INVALID_REQUEST, message); }
}
