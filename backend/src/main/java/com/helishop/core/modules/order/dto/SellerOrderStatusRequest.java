package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Chuyển một bước xử lý của đơn; gửi trạng thái đã đọc để phát hiện dữ liệu cũ")
public record SellerOrderStatusRequest(@NotNull OrderStatus expectedStatus, @NotNull OrderStatus status) {}
