package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Đơn bán mới nhất, trạng thái có thể chuyển và giải thích về thanh toán/giao hàng")
public record SellerOrderResponse(OrderResponse order, List<OrderStatus> allowedStatuses, String actionNote, LocalDateTime updatedAt) {}
