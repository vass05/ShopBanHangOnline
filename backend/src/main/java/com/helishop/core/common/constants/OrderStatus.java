package com.helishop.core.common.constants;

public enum OrderStatus {
    PENDING,//đơn hàng mới tạo
    CONFIRMED,//đơn hàng đã xác nhận
    PROCESSING,//đơn hàng đang xử lý
    SHIPPING,//đơn hàng đang giao hàng
    DELIVERED,//đơn hàng đã giao hàng
    CANCELLED,//đơn hàng đã hủy
    RETURNED//đơn hàng đã trả hàng
}
