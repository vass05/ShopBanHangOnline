# LỘ TRÌNH DỰ ÁN TỔNG THỂ: HELISHOP E-COMMERCE

Tài liệu quản lý vòng đời phát triển, các giai đoạn (Phases), các dự án (Projects) và Sprint chi tiết từ khởi tạo đến bàn giao hoàn thiện.

## Cập nhật rà soát tích hợp — 13/11/2026

**Chưa đạt nghiệm thu tích hợp frontend/backend cho luồng mua hàng.** Các mốc “100%” bên dưới ghi nhận bàn giao lịch sử từng phần, không chứng minh luồng trình duyệt → API → dữ liệu production đã hoàn chỉnh.

Rà soát mã nguồn xác nhận giỏ hàng và đơn mua đang lưu cục bộ; checkout chưa gọi API tạo đơn, payload VNPAY lệch và lỗi thanh toán có fallback thành công. Cần đồng bộ hợp đồng, quyền sở hữu dữ liệu, phí/voucher, nhiều shop và cấu hình Docker.

Kế hoạch chi tiết và tiêu chí nghiệm thu: [implementation_plan.md](../implementation_plan.md). Trạng thái các giai đoạn dưới đây: **đã lập kế hoạch, chưa triển khai**.

| Thứ tự | Giai đoạn tiếp theo | Ưu tiên |
| --- | --- | --- |
| 1 | Chốt hợp đồng API, sửa quyền truy cập và phiên đăng nhập | P0 |
| 2 | Nối giỏ hàng frontend với Redis Cart API | P0 |
| 3 | Checkout COD, tiền hàng server, nhiều shop và đơn mua thật | P0 |
| 4 | VNPAY với orderCode thật, xác minh return và trạng thái IPN | P0 |
| 5 | Catalog phân trang/lọc server và Docker/ảnh upload | P1 |
| 6 | Browser E2E, dịch vụ thật, CI và bàn giao | P0 cho phát hành |

Kiểm tra baseline lần này: backend **103/103 tests pass**, frontend **production build pass**. Chưa chạy browser E2E hoặc nghiệm thu toàn cụm Docker production.

---

## 🧭 TỔNG QUAN CÁC GIAI ĐOẠN (PROJECTS)

```mermaid
graph TD
    P1[Project 1: HeliShop Core E-Commerce Backend] -->|Hoàn thành 100%| P2[Project 2: Shopee E-Commerce Integration & Automation]
    P2 -->|Cần nghiệm thu tích hợp theo kế hoạch 13/11/2026| I[Đồng bộ Frontend / Backend]
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
