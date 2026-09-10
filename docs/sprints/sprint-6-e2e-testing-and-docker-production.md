# BÁO CÁO HOÀN THÀNH SPRINT 6: END-TO-END TESTING, CI/CD & ĐÓNG GÓI DOCKER PRODUCTION

- **Dự án**: Project 2 - Shopee E-Commerce Integration & Automation (Sprint Chốt Chặn Bàn Giao)
- **Mã Sprint**: Sprint 6 (E2E Testing, Stress Concurrency, Fault Tolerance & Docker Multi-stage Production)
- **Trạng thái**: ✅ **HOÀN THÀNH 100%**
- **Definition of Done**:
  - 103/103 bài kiểm thử Backend PASSED (100% Passed, 0 Errors, 0 Failures).
  - 200 Threads Concurrency Test đạt chuẩn: Không deadlock trên Redis Cart, Khóa bi quan (`PESSIMISTIC_WRITE`) triệt tiêu hoàn toàn Race Condition và âm kho.
  - Fault Tolerance E2E: Mô phỏng sập dịch vụ SMTP -> Message chuyển an toàn vào `order.email.dlq`, API khách hàng không bị nghẽn.
  - Multi-stage Dockerfile cho Frontend (Node 20 Alpine -> Nginx 1.27 Alpine).
  - `docker-compose.production.yml` hợp lệ 100%, tích hợp 5 dịch vụ khép kín (`frontend`, `backend`, `mysql`, `redis`, `rabbitmq`).

---

## 🧪 1. KẾT QUẢ KIỂM THỬ TẢI TRỌNG & CHỊU LỖI (E2E & STRESS TEST)

### 1.1. Luồng Thanh toán E2E & Idempotent Webhook ([PaymentAndEmailDlqE2ETest.java](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/backend/src/test/java/com/helishop/core/e2e/PaymentAndEmailDlqE2ETest.java))
- **Luồng hoàn chỉnh**: Giả lập VNPAY gửi Webhook IPN kèm chữ ký HMAC-SHA512 hợp lệ:
  - Backend xác thực chữ ký số thành công.
  - Cập nhật đơn hàng sang `payment_status = PAID` và `order_status = PROCESSING`.
  - Ghi nhận bản ghi `payment_transactions` chi tiết (mã giao dịch, số tiền, cổng thanh toán).
  - Kích hoạt `OrderPaidEvent` gửi qua `order.direct.exchange` với routing key `order.paid.email`.
- **Tính Idempotent chống trùng lặp**: Gửi Webhook IPN lần 2 cho cùng giao dịch đã `PAID` $\rightarrow$ Hệ thống lập tức nhận diện và trả về mã `{"RspCode": "02", "Message": "Order already confirmed"}` mà không sinh thêm transaction hay gửi lặp email.

### 1.2. Kiểm thử Chịu lỗi (Fault Tolerance & Dead Letter Queue)
- **Kịch bản sự cố**: Mô phỏng toàn bộ dịch vụ SMTP Server bị sập kết nối (`MessagingException: Connection refused`).
- **Cơ chế phòng hộ**:
  - `OrderEmailConsumer` bắt lỗi và ném `RuntimeException` kích hoạt cơ chế Retry 3 lần của Spring AMQP.
  - Sau 3 lần thử lại thất bại, message tự động được chuyển hướng sang Dead Letter Exchange (`order.dlx.exchange`) và lưu trữ an toàn tại `order.email.dlq`.
  - Luồng giao dịch thanh toán và mua hàng của khách hàng **hoàn toàn thông suốt**, không bị crash, không bị nghẽn thread.

### 1.3. Kiểm thử Đa luồng Đồng thời (Concurrency Stress Test - 200 Threads) ([CartAndStockConcurrencyStressTest.java](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/backend/src/test/java/com/helishop/core/e2e/CartAndStockConcurrencyStressTest.java))
- **Test Concurrency Cart (200 requests đồng thời)**:
  - Khởi tạo 200 luồng đồng thời thao tác ghi nhận/sửa đổi trên Redis Cart HASH (`cart:user:{userId}`).
  - Kết quả: **200/200 requests thành công**, không có xung đột dữ liệu, tốc độ phản hồi tính bằng mili-giây.
- **Test Concurrency Checkout (Chống âm kho Flash Sale)**:
  - Cấu hình 1 SKU có số lượng tồn kho giới hạn duy nhất **10 sản phẩm**.
  - Dùng `CountDownLatch` bắn đồng loạt **200 requests checkout** tranh mua cùng một tích tắc.
  - Nhờ khóa bi quan `findByIdWithLock(skuId)` (`PESSIMISTIC_WRITE`):
    - Đúng **10 requests đầu tiên** đặt hàng thành công.
    - **190 requests còn lại** bị từ chối an toàn với ngoại lệ `InsufficientStockException`.
    - Số lượng tồn kho trong cơ sở dữ liệu hạ về chính xác bằng **0**, **TUYỆT ĐỐI KHÔNG BỊ ÂM KHO**.

---

## 🐳 2. HỆ SINH THÁI DOCKER PRODUCTION HOÀN CHỈNH

### 2.1. Dockerfile Đa Tầng Frontend ([frontend/Dockerfile](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/frontend/Dockerfile))
- **Stage 1 (Builder)**: `node:20-alpine`, `npm ci`, build gói phân phối tĩnh tối ưu bằng `vite build`.
- **Stage 2 (Runner)**: `nginx:1.27-alpine` siêu nhẹ, tích hợp cấu hình [frontend/nginx.conf](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/frontend/nginx.conf):
  - Hỗ trợ Single Page Application (SPA) với `try_files $uri $uri/ /index.html;`.
  - Nén dữ liệu Gzip tự động.
  - Cache static assets (`max-age=1y`).
  - Reverse Proxy chuyển tiếp `/api/` trực tiếp sang container Backend:
    ```nginx
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    ```

### 2.2. Docker Compose Production ([docker-compose.production.yml](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/docker-compose.production.yml))
Cụm 5 container cô lập trong mạng riêng `helishop_prod_network`:

| Container | Dịch vụ & Image | Cổng Publish | Tính năng & Lưu trữ |
| :--- | :--- | :---: | :--- |
| `helishop_prod_frontend` | Nginx 1.27 Alpine + Vite SPA | `80:80` | Reverse proxy `/api`, SPA fallback, Healthcheck |
| `helishop_prod_backend` | Spring Boot 3 / Temurin 21 JRE | `8080:8080` | Non-root security user, JVM Container flags, Swagger |
| `helishop_prod_mysql` | MySQL 8.0 Server | `3307:3306` | utf8mb4_unicode_ci, Timezone +07:00, Volume Persistent |
| `helishop_prod_redis` | Redis 7 Alpine | `6379:6379` | `appendonly yes` (AOF), Volume Persistent |
| `helishop_prod_rabbitmq` | RabbitMQ 3.13 Management | `5672`, `15672` | DLQ, DLX Exchange, Volume Persistent |

Lệnh triển khai Production 1 câu lệnh duy nhất:
```powershell
docker compose -f docker-compose.production.yml up -d --build
```

---

## 📊 3. TỔNG KẾT DỰ ÁN PROJECT 2: SHOPEE E-COMMERCE INTEGRATION & AUTOMATION

Sau 6 Sprint thi công liên tục và nghiêm ngặt, toàn bộ **Project 2** đã hoàn thành xuất sắc 100% mục tiêu kiến trúc và tính năng:
1. **Sprint 1**: Hạ tầng RabbitMQ 3.13, Redis AOF Persistent & Redis Cart Engine (TTL 30 ngày).
2. **Sprint 2**: Cổng thanh toán VNPAY Sandbox, Ký số HMAC-SHA512 & Idempotent Webhook.
3. **Sprint 3**: Async Email Worker, Consumer Retry x3, Dead Letter Queue & Upload Cloudinary 1:1.
4. **Sprint 4**: Frontend Core React 18, Tailwind, Zustand & Axios Interceptor `failedQueue` chống spam 401.
5. **Sprint 5**: Giao diện Toàn diện Chuẩn Shopee (Tone Xanh Biển Ocean Blue `#0284C7`), PDP 2 cấp SKU, Giỏ hàng gom nhóm Shop, Sticky Checkout & 6 Tab Đơn hàng.
6. **Sprint 6**: E2E Integration Test, Fault Tolerance DLQ, Concurrency Stress Test 200 Threads & Đóng gói Docker Compose Production.
