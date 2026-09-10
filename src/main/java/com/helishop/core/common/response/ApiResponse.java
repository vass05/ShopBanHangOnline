package com.helishop.core.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cấu trúc phản hồi JSON chuẩn toàn hệ thống")
public class ApiResponse<T> {

    @Schema(description = "Mã trạng thái HTTP code", example = "200")
    @Builder.Default
    private int code = 200;

    @Schema(description = "Thông điệp thông báo kết quả xử lý", example = "Thành công")
    private String message;

    @Schema(description = "Dữ liệu payload phản hồi (Generic object/list)")
    private T data;

    @Schema(description = "Bản đồ chi tiết lỗi validation từng trường (nếu có)")
    private java.util.Map<String, String> errors;

    @Schema(description = "Thời điểm xử lý phản hồi", example = "2026-09-10T14:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, java.util.Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
