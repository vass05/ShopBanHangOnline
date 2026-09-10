# LỘ TRÌNH DỰ ÁN TỔNG THỂ: HELISHOP E-COMMERCE

Tài liệu quản lý vòng đời phát triển, các giai đoạn (Phases), các dự án (Projects) và Sprint chi tiết từ khởi tạo đến bàn giao hoàn thiện.

---

## 🧭 TỔNG QUAN CÁC GIAI ĐOẠN (PROJECTS)

```mermaid
graph TD
    P1[Project 1: HeliShop Core E-Commerce Backend] -->|Hoàn thành 100%| P2[Project 2: Shopee E-Commerce Integration & Automation]
    P2 -->|Đang thi công Sprint 1/6| P3[Project 3: Production Hardening, Microservices & CI/CD Cloud]
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
- **Trạng thái**: 🚀 **ĐANG THI CÔNG (Sprint 1 Hoàn thành, Chuẩn bị Sprint 2)**
- **Kiến trúc trọng tâm**: Redis Cart Hash Map (TTL 30 ngày), RabbitMQ Message Broker với Dead Letter Queue (DLQ), VNPAY Webhook IPN, Async Email Worker, Media Cloudinary/S3, React 18 Shopee UI.

| Sprint | Nội dung thực hiện | Thời lượng | Trạng thái | Commit / Ghi chú |
| :---: | :--- | :---: | :---: | :--- |
| **Sprint 1** | **Hạ tầng RabbitMQ 3.13, Redis AOF Persistent, Redis Cart Engine Shopee** | 4–5 ngày | ✅ **Hoàn thành** | Commit `fe4d48c` (Đã push lên GitHub) |
| **Sprint 2** | **Order Event Publishing, RabbitMQ Producer & Async Email Worker (Retry x3 + DLQ)** | 4–5 ngày | ⏳ *Kế tiếp* | Đã sẵn sàng file thiết kế Sprint 2 |
| **Sprint 3** | **Tích hợp Cổng thanh toán VNPAY, Sandbox URL & Webhook IPN xử lý HMAC-SHA512** | 4–5 ngày | 📋 Chưa bắt đầu | Xử lý Idempotency Key & cập nhật Order |
| **Sprint 4** | **Hủy đơn hàng, Hoàn trả kho nguyên tử & Dead Letter Queue (DLQ) Fallback Engine** | 3–4 ngày | 📋 Chưa bắt đầu | Xử lý Dead Letter Messages & Alerting |
| **Sprint 5** | **Cloudinary / AWS S3 Media Upload & Quản lý Biến thể SKU đa thuộc tính** | 4–5 ngày | 📋 Chưa bắt đầu | Upload đa ảnh sản phẩm & SKU Variant |
| **Sprint 6** | **Giao diện React 18 + Vite Frontend, Silent Refresh Token & Shopee Cart UI** | 5–6 ngày | 📋 Chưa bắt đầu | Giao diện chuẩn Shopee phân nhóm Shop |

---

## 🎯 DEFINITION OF DONE (DOD) CHO TỪNG SPRINT
1. **Mã nguồn sạch**: Biên dịch thành công, không phát sinh cảnh báo quan trọng.
2. **Kiểm thử tự động**: 100% test case (Unit test & MockMvc Integration test) PASSED.
3. **Tài liệu hóa**: Cập nhật Swagger UI, các file Markdown trong `docs/`.
4. **Git Remote Push**: Toàn bộ mã nguồn phải được commit theo chuẩn Conventional Commits và **đẩy trực tiếp lên `origin/main`**.
