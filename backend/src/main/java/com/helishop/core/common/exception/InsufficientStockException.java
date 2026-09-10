package com.helishop.core.common.exception;

public class InsufficientStockException extends AppException {

    public InsufficientStockException(String skuCode, Integer requested, Integer available) {
        super(
            ErrorCode.INSUFFICIENT_STOCK,
            String.format("Sản phẩm mã '%s' không đủ số lượng tồn kho (yêu cầu: %d, hiện có: %d)", skuCode, requested, available)
        );
    }

    public InsufficientStockException(String message) {
        super(ErrorCode.INSUFFICIENT_STOCK, message);
    }
}
