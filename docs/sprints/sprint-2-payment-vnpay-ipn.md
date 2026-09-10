# SPRINT 2 SUMMARY: PAYMENT GATEWAY (VNPAY SANDBOX) & IDEMPOTENT WEBHOOK

- **Dự án**: Project 2 - Shopee E-Commerce Integration & Automation
- **Thời lượng**: 5 ngày
- **Trạng thái**: ✅ **HOÀN THÀNH 100%**
- **Quy trình chuẩn**: 5/5 bước xử lý IPN & Bắn sự kiện RabbitMQ

---

## 1. MỤC TIÊU SPRINT 2
1. **Tích hợp VNPAY Core**:
   - `VnPayConfig`: cấu hình `vnp_TmnCode`, `vnp_HashSecret`, `vnp_PayUrl`, `vnp_ReturnUrl`, `vnp_IpnUrl`.
   - `VnPayUtils`: thuật toán ký số HMAC-SHA512, sắp xếp các trường theo bảng chữ cái (alphabetical sort), tạo URL thanh toán chuẩn VNPAY 2.1.0.
2. **Xử lý Webhook IPN (Server-to-Server)** với 5 bước chuẩn:
   - **Bước 1: Validate Checksum**: Xác minh chữ ký số HMAC-SHA512 tránh giả mạo request (nếu sai trả `97 - Invalid Checksum`).
   - **Bước 2: Idempotency Check**: Nếu đơn hàng đã `payment_status == 'PAID'`, trả ngay `{"RspCode": "02", "Message": "Order already confirmed"}` mà không ghi đè DB hay bắn lặp message.
   - **Bước 3: Update State**: Nếu `vnp_ResponseCode == "00"` -> cập nhật `orders.payment_status = 'PAID'`, `orders.order_status = 'PROCESSING'`.
   - **Bước 4: Record Txn**: Ghi toàn bộ dữ liệu phản hồi vào bảng `payment_transactions`.
   - **Bước 5: Fire Event**: Bắn tin nhắn `OrderPaidEvent` vào RabbitMQ Exchange `order.direct.exchange` với routing key `order.paid.email`.

---

## 2. DANH SÁCH REST API ĐÃ TRIỂN KHAI

| Method | Endpoint | Quyền hạn | Mô tả |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/payments/create-vnpay-url` | `ROLE_CUSTOMER` | Sinh URL thanh toán VNPAY kèm chữ ký HMAC-SHA512 |
| `GET` | `/api/v1/payments/vnpay-ipn` | `Public (permitAll)` | Webhook IPN Server-to-Server từ VNPAY (Trả JSON RspCode) |
| `GET` | `/api/v1/payments/vnpay-return` | `Public (permitAll)` | Tiếp nhận trình duyệt người dùng quay lại sau thanh toán |

---

## 3. KẾT QUẢ KIỂM THỬ (15 TESTS MỚI - 100% PASSED)
- **VnPayUtilsTest**: 4/4 passed (Tính toán HMAC-SHA512, sắp xếp tham số, mã hóa URL, trích xuất IP).
- **PaymentServiceTest**: 7/7 passed (Tạo URL, kiểm tra trạng thái đơn hàng, sai chữ ký 97, không tìm thấy đơn 01, sai số tiền 04, Idempotent 02, thành công 00 kèm lưu Transaction và phát RabbitMQ event).
- **PaymentIpnIntegrationTest**: 4/4 passed (Truy cập công khai IPN, kiểm tra response JSON RspCode, xác thực 401 khi tạo URL không token, 200 khi có token).
- **Toàn bộ Test Suite Dự Án**: **89 tests passed - 0 errors - 0 failures**.
