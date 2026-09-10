package com.helishop.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Khóa thông báo lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1002, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1003, "Người dùng đã tồn tại trên hệ thống", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1004, "Chưa xác thực danh tính hoặc phiên đã hết hạn", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "Bạn không có quyền truy cập chức năng này", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1006, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1007, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    SKU_OUT_OF_STOCK(1008, "Sản phẩm trong kho không đủ đáp ứng", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(1008, "Sản phẩm trong kho không đủ đáp ứng", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT(1009, "Xung đột đồng thời dữ liệu khi thanh toán/cập nhật", HttpStatus.CONFLICT),
    INVALID_REQUEST(1010, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1011, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
