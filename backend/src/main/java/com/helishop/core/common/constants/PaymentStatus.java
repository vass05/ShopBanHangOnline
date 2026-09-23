package com.helishop.core.common.constants;

public enum PaymentStatus {
    UNPAID,//chưa thanh toán
    PENDING,//đơn hàng đang chờ thanh toán
    PAID,//đã thanh toán
    FAILED,//thanh toán thất bại
    REFUNDED//đã hoàn tiền
}
