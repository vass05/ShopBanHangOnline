# SPRINT 2 PLAN: ORDER EVENT PUBLISHING & ASYNC EMAIL WORKER

- **Dự án**: Project 2 - Shopee E-Commerce Integration & Automation
- **Thời lượng ước tính**: 4–5 ngày
- **Trạng thái**: ⏳ **CHUẨN BỊ TRIỂN KHAI**

---

## 1. MỤC TIÊU SPRINT 2

1. **Order Event Model & Publisher**:
   - Định nghĩa DTO sự kiện `OrderPaidEvent` chứa: `orderId`, `orderCode`, `customerEmail`, `customerName`, `totalAmount`, `paymentMethod`, `paidAt`, danh sách mặt hàng `List<OrderItemDto>`.
   - Tạo `OrderEventPublisher` sử dụng `RabbitTemplate` để đẩy message tới exchange `order.direct.exchange` với routing key `order.paid.email` và `order.paid.seller`.

2. **Async Email Consumer & Worker Service**:
   - Viết `@Service` `OrderEmailConsumer` lắng nghe queue `order.email.queue`.
   - Xử lý bất đồng bộ tạo `MimeMessage` gửi hóa đơn điện tử / thông báo xác nhận thanh toán thành công tới người mua.
   - Định dạng nội dung email HTML responsive gồm thông tin đơn hàng, danh sách sản phẩm và tổng tiền.

3. **Cơ chế Retry & Chuyển hướng DLQ tự động**:
   - Cấu hình Spring AMQP Retry: tối đa 3 lần thử lại (`max-attempts: 3`).
   - Exponential Backoff: khoảng cách giữa các lần thử lại tăng dần (1s -> 2s -> 4s).
   - Khi vượt quá 3 lần gửi thất bại (ví dụ SMTP server down, timeout): Message tự động chuyển hướng sang `order.dlx.exchange` -> `order.email.dlq`.

4. **Kiểm thử tự động**:
   - Unit test cho `OrderEventPublisher`.
   - Mock test cho `OrderEmailConsumer`.
   - Integration test luồng Retry và Dead Letter routing.

---

## 2. KẾT NỐI VÀO LUỒNG CHECKOUT
- Khi đơn hàng được thanh toán thành công (hoặc checkout COD):
  - Kích hoạt sự kiện `orderEventPublisher.publishOrderPaidEvent(...)`.
  - Không làm chậm luồng HTTP request của khách hàng (hoàn toàn non-blocking).
