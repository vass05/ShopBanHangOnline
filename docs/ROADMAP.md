# LỘ TRÌNH DỰ ÁN TỔNG THỂ: HELISHOP E-COMMERCE

## Trạng thái tích hợp — 13/11/2026

Luồng khách hàng đã được kiểm chứng trên Docker cục bộ: catalog → xác thực/OTP → giỏ Redis → địa chỉ/checkout → đơn mua/hủy → Return/IPN → email/ảnh đại diện. Các chức năng 01–09 đã merge qua PR #1–#9; chức năng 10 trên `test/browser-checkout-and-ci` bổ sung browser E2E và ba job CI, bàn giao qua PR riêng.

Kết quả: **152/152 backend tests, 31/31 frontend tests/build, 6/6 browser E2E pass**. Cụm thử web 18000 dùng MySQL, Redis, RabbitMQ, Nginx và Mailpit thật, volume riêng; không thay đổi container dev. Chi tiết từng chức năng và nhánh/PR: [implementation_plan.md](../implementation_plan.md). Tổng kết: [integration-10-browser-ci.md](sprints/integration-10-browser-ci.md).

Đợt này hoàn thành tích hợp cục bộ, chưa nghiệm thu nhà cung cấp thật hoặc phát hành Internet. Không đánh đồng các mốc lịch sử “100%” phía dưới với việc production đã sẵn sàng.

| Phần tiếp theo | Điều kiện |
| --- | --- |
| VNPAY/SMTP/Cloudinary thật | Merchant/secret hợp lệ, callback HTTPS công khai, kiểm chứng giao dịch và lỗi nhà cung cấp |
| Vận hành Internet | Domain/TLS, backup và thử khôi phục, migration schema có kiểm soát, giám sát outbox/DLQ |
| Mở rộng seller/admin | Dashboard, kênh nhận seller-notify, đánh giá/chat/vận chuyển ngoài; lập kế hoạch riêng |
| Thanh toán gộp nhiều shop | Payment-group riêng; hiện hỗ trợ thanh toán từng đơn theo shop |

Các phần Project/Sprint phía dưới lưu lịch sử phát triển. Baseline trước đợt sửa `acdd254` có 103 backend tests và chưa nối giỏ/checkout; trạng thái hiện hành là kết quả ở trên.

---
## 🧭 TỔNG QUAN CÁC GIAI ĐOẠN (PROJECTS)

```mermaid
graph TD
    P1[Project 1: HeliShop Core E-Commerce Backend] -->|Hoàn thành 100%| P2[Project 2: Shopee E-Commerce Integration & Automation]
    P2 -->|Đã nghiệm thu tích hợp cục bộ 13/11/2026| I[Đồng bộ Frontend / Backend]
    I --> P3[Project 3: Production Hardening, Microservices & CI/CD Cloud]
```

---

## 🌟 PROJECT 1: CORE E-COMMERCE BACKEND PLATFORM
- **Trạng thái**: ✅ **100% HOÀN THÀNH**
- **Commit Baseline**: `5d0ba9c`
- **Mục tiêu**: Xây dựng nền tảng backend Spring Boot 3.3.5 / Java 21, xác thực JWT, danh mục sản phẩm, khóa bi quan trừ kho và Redis Caching.

| Sprint | Nội dung thực hiện | Thời lượng | Trạng thái |
| :---: | :--- | :---: | :---: |
| **Sprint 1** | Thiết lập dự án, Docker (MySQL + Redis), BaseEntity, User, Shop | 3–4 ngày | ✅ Hoàn thành |
| **Sprint 2** | Spring Security 6, Stateless JWT, Refresh Token Redis, RBAC | 3–4 ngày | ✅ Hoàn thành |
| **Sprint 3** | Quản lý Hàng hóa & Danh mục đa cấp, Dynamic Filter JPA Spec, N+1 Query | 4–5 ngày | ✅ Hoàn thành |
| **Sprint 4** | Đặt hàng an toàn, Khóa bi quan (Pessimistic Lock), Chống âm kho | 3–4 ngày | ✅ Hoàn thành |
| **Sprint 5** | Tối ưu hiệu năng, Redis Distributed Cache, Cache-Aside Pattern | 3–4 ngày | ✅ Hoàn thành |
| **Sprint 6** | Dockerfile Multi-stage, Swagger OpenAPI 100%, Unit & Integration Test | 3–4 ngày | ✅ Hoàn thành |

---

## ⚡ PROJECT 2: SHOPEE E-COMMERCE INTEGRATION & AUTOMATION
- **Trạng thái**: ✅ **100% HOÀN THÀNH TOÀN DIỆN (6/6 Sprints Passed)**
- **Kiến trúc trọng tâm**: Redis Cart Hash Map (TTL 30 ngày), RabbitMQ Message Broker với Dead Letter Queue (DLQ), VNPAY Webhook IPN, Async Email Worker, Media Cloudinary/S3, React 18 Shopee UI, E2E Stress Test 200 Threads & Docker Compose Production.

| Sprint | Nội dung thực hiện | Thời lượng | Trạng thái | Commit / Ghi chú |
| :---: | :--- | :---: | :---: | :--- |
| **Sprint 1** | **Hạ tầng RabbitMQ 3.13, Redis AOF Persistent, Redis Cart Engine Shopee** | 4–5 ngày | ✅ **Hoàn thành** | Commit `fe4d48c` (Đã push lên GitHub) |
| **Sprint 2** | **Cổng thanh toán VNPAY Sandbox, Ký số HMAC-SHA512 & Idempotent Webhook IPN** | 5 ngày | ✅ **Hoàn thành** | Commit `5b5bddb` (Đã push lên GitHub) |
| **Sprint 3** | **Async Email Worker, RabbitMQ Consumer, Retry x3 & Media Upload Cloudinary 1:1** | 4 ngày | ✅ **Hoàn thành** | Commit `4816bc8` (Đã push lên GitHub) |
| **Sprint 4** | **Frontend Core - React 18, Tailwind, Zustand & Axios failedQueue Silent Refresh** | 5 ngày | ✅ **Hoàn thành** | React 18, Vite, Shadcn UI, failedQueue Interceptor |
| **Sprint 5** | **Giao diện Toàn diện Chuẩn Shopee (Màu chủ đạo HeliShop Xanh Biển Ocean Blue)** | 5–6 ngày | ✅ **Hoàn thành** | Header Autocomplete, PDP 2 cấp SKU, Cart phân nhóm Shop, Sticky Checkout, VNPAY & 6 Tab Đơn hàng |
| **Sprint 6** | **End-to-End Testing, CI/CD & Đóng gói Docker Production Toàn diện** | 4 ngày | ✅ **Hoàn thành** | E2E VNPAY & DLQ, 200 Threads Stress Concurrency, Nginx Reverse Proxy & Docker Compose Prod |

---

## 🎯 DEFINITION OF DONE (DOD) CHO TỪNG SPRINT
1. **Mã nguồn sạch**: Biên dịch thành công, không phát sinh cảnh báo quan trọng.
2. **Kiểm thử tự động**: 100% test case (Unit test & MockMvc Integration test) PASSED.
3. **Tài liệu hóa**: Cập nhật Swagger UI, các file Markdown trong `docs/`.
4. **Git Remote Push**: Toàn bộ mã nguồn phải được commit theo chuẩn Conventional Commits và **đẩy trực tiếp lên `origin/main`**.
