# HeliShop - Enterprise E-Commerce Platform

<div align="center">

![HeliShop Banner](https://images.unsplash.com/photo-1557821552-17105176677c?q=80&w=1600&auto=format&fit=crop)

### Nền Tảng Thương Mại Điện Tử Toàn Diện Chuẩn Doanh Nghiệp
*Mô hình Shopee Mall • Kiến trúc Domain-Driven Layered • Xử lý tải cao & Chống âm kho Flash Sale*

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7_Alpine-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Production_Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Backend_Tests-103%2F103_Passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)]()
[![Swagger](https://img.shields.io/badge/OpenAPI_3.0-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui/index.html)

</div>

---

## Mục Lục
- [1. Tổng Quan Dự Án](#1-tổng-quan-dự-án)
- [2. Kiến Trúc Hệ Thống & Luồng Dữ Liệu](#2-kiến-trúc-hệ-thống--luồng-dữ-liệu)
- [3. Các Phân Hệ & Điểm Sáng Kỹ Thuật](#3-các-phân-hệ--điểm-sáng-kỹ-thuật)
- [4. Ngăn Xếp Công Nghệ (Tech Stack)](#4-ngăn-xếp-công-nghệ-tech-stack)
- [5. Cấu Trúc Thư Mục Dự Án](#5-cấu-trúc-thư-mục-dự-án)
- [6. Hướng Dẫn Cài Đặt & Khởi Chạy Nhanh](#6-hướng-dẫn-cài-đặt--khởi-chạy-nhanh)
- [7. Thông Số Cổng Dịch Vụ & Biến Môi Trường](#7-thông-số-cổng-dịch-vụ--biến-môi-trường)
- [8. Kiểm Thử & Đảm Bảo Chất Lượng (QA)](#8-kiểm-thử--đảm-bảo-chất-lượng-qa)
- [9. Tài Liệu API (OpenAPI 3.0 / Swagger)](#9-tài-liệu-api-openapi-30--swagger)
- [10. Lộ Trình Phát Triển (Roadmap)](#10-lộ-trình-phát-triển-roadmap)
- [11. Bản Quyền & Giấy Phép (License)](#11-bản-quyền--giấy-phép-license)

---

## 1. Tổng Quan Dự Án

**HeliShop** là nền tảng thương mại điện tử đa người bán (Multi-vendor E-Commerce) được thiết kế và xây dựng theo chuẩn mực doanh nghiệp. Hệ thống giải quyết các bài toán kỹ thuật trọng yếu trong thương mại điện tử quy mô lớn:

1. **Hiệu năng Giỏ hàng Phân tán**: Động cơ giỏ hàng vận hành trên **Redis Hash Map** cho tốc độ phản hồi mili-giây, gom nhóm theo từng Shop và duy trì dữ liệu phiên 30 ngày.
2. **Bảo vệ Tồn kho Flash Sale Chống Âm Kho**: Cơ chế **Khóa bi quan (`PESSIMISTIC_WRITE`)** trên từng biến thể SKU, triệt tiêu race condition kể cả khi có hàng trăm luồng đặt hàng đồng thời.
3. **Giao dịch Phân tán & Khả năng Chịu lỗi (Fault Tolerance)**: Sử dụng **RabbitMQ 3.13** định tuyến sự kiện đơn hàng bất đồng bộ, cơ chế retry 3 lần và chuyển tiếp an toàn vào **Dead Letter Queue (DLQ)** khi hệ thống mail gặp sự cố.
4. **Cổng Thanh Toán Trực Tuyến An Toàn**: Tích hợp **VNPAY Sandbox** xác thực chữ ký số **HMAC-SHA512**, Webhook IPN xử lý bất đồng bộ chuẩn **Idempotent** (chống xử lý lặp giao dịch).
5. **Giao Diện Shopee Mall Hiện Đại**: Ứng dụng **React 18 SPA**, bộ nhận diện thương hiệu **Ocean Blue (`#0284C7`)**, cơ chế lọc danh mục đệ quy đa tầng, PDP biến thể 2 cấp và **100% kết nối Backend REST API thật**.

---

## 2. Kiến Trúc Hệ Thống & Luồng Dữ Liệu

### 2.1. Sơ Đồ Kiến Trúc Tổng Thể (System Architecture)

```mermaid
flowchart TB
    subgraph ClientLayer ["Client & Network Gateway"]
        Browser["Trình duyệt Khách hàng / Quản trị viên"]
        Nginx["Nginx 1.27 Reverse Proxy (Port 80)"]
        SPA["React 18 + Vite SPA (Ocean Blue UI)"]
        Axios["Axios Interceptor (Silent Refresh Token)"]
    end

    subgraph BackendCore ["HeliShop Core Backend (Spring Boot 3.3.5 / Java 21)"]
        Security["Spring Security 6 (Stateless JWT + RBAC)"]
        Controllers["REST API Controllers (Swagger OpenAPI 3.0)"]
        ServiceLayer["Business Logic Modules (Order, Product, Cart, Payment)"]
        JPA["Spring Data JPA + Hibernate (Optimized EntityGraph)"]
    end

    subgraph DistributedStorage ["Lưu Trữ Dữ Liệu & Bộ Nhớ Đệm"]
        MySQL[("MySQL 8.0\n(Source of Truth\nPessimistic Locks)")]
        Redis[("Redis 7 Alpine\n(AOF Persistent\nCart Hash + Caching)")]
    end

    subgraph AsyncBroker ["Xử Lý Bất Đồng Bộ & Bên Thứ Ba"]
        RabbitMQ{{"RabbitMQ 3.13 Broker\n(order.direct.exchange)"}}
        DLQ[("Dead Letter Queue\norder.email.dlq")]
        EmailWorker["Async Email Worker\n(HTML MimeMessage)"]
        VNPAY["VNPAY Payment Gateway\n(HMAC-SHA512 Webhook IPN)"]
    end

    Browser -->|HTTP Requests| Nginx
    Nginx -->|Static Assets| SPA
    SPA -->|API Calls| Axios
    Axios -->|Proxy /api/| Nginx
    Nginx -->|Forward| Controllers

    Controllers --> Security
    Security --> ServiceLayer
    ServiceLayer --> JPA
    JPA -->|Read/Write ACID & FOR UPDATE| MySQL

    ServiceLayer -->|Hash Ops & Cache-Aside| Redis
    ServiceLayer -->|Publish OrderPaidEvent| RabbitMQ
    RabbitMQ -->|Direct Routing| EmailWorker
    RabbitMQ -.->|Thất bại sau 3 lần retry| DLQ

    VNPAY -->|Server-to-Server IPN| Controllers
```

---

### 2.2. Luồng Checkout Nguyên Tử & Chống Âm Kho (Hybrid Transaction Flow)

```mermaid
sequenceDiagram
    autonumber
    actor User as Khách Hàng
    participant Cart as Redis Cart Hash
    participant OrderSvc as Order Service (@Transactional)
    participant DB as MySQL 8.0 (SKU Inventory)
    participant Broker as RabbitMQ Broker
    participant VNPAY as Cổng VNPAY

    User->>Cart: 1. Chọn các sản phẩm và nhấn "Đặt Hàng"
    Cart-->>OrderSvc: Trích xuất danh sách CartItemDto theo skuId
    Note over OrderSvc,DB: Khóa bi quan nguyên tử từng SKU
    loop Khóa tồn kho từng biến thể SKU
        OrderSvc->>DB: SELECT * FROM product_skus WHERE id = ? FOR UPDATE
        DB-->>OrderSvc: Khóa dòng (Row-level Lock) thành công
        OrderSvc->>OrderSvc: Kiểm tra: Tồn kho >= Số lượng đặt?
        alt Thiếu hàng / Hết tồn kho
            OrderSvc-->>User: Ném InsufficientStockException (HTTP 409 Conflict)
        else Đủ hàng
            OrderSvc->>DB: UPDATE product_skus SET stock_quantity = stock_quantity - ?
        end
    end
    OrderSvc->>DB: INSERT INTO orders & order_items (Snapshot giá, tên, SKU)
    OrderSvc->>Cart: HDEL cart:user:{userId} skuId1 skuId2...
    alt Thanh toán Online (VNPAY)
        OrderSvc->>VNPAY: Khởi tạo URL giao dịch kèm chữ ký HMAC-SHA512
        OrderSvc-->>User: Điều hướng tới cổng thanh toán VNPAY
    else Thanh toán khi nhận hàng (COD)
        OrderSvc->>Broker: Publish OrderCreatedEvent (order.email.queue)
        OrderSvc-->>User: Trả về đơn hàng thành công (Trạng thái PENDING)
    end
```

---

### 2.3. Cơ Chế Chịu Lỗi Email Worker Với Dead Letter Queue (DLQ)

```mermaid
graph LR
    Publisher["Order Service"] -->|Publish Sự Kiện| DirectExchange["order.direct.exchange"]
    DirectExchange -->|Routing: order.paid.email| EmailQueue["order.email.queue"]
    
    EmailQueue -->|Thử lần 1, 2, 3| Consumer["OrderEmailConsumer\n(Spring AMQP)"]
    Consumer -->|Gửi thành công| SMTP["SMTP Server\n(Google Mail)"]
    
    Consumer -.->|Lỗi kết nối / SMTP sự cố sau 3 lần retry| DLX["order.dlx.exchange"]
    DLX -->|Routing: order.email.dlq| DLQ["order.email.dlq\n(Lưu trữ an toàn để replay)"]
```

---

## 3. Các Phân Hệ & Điểm Sáng Kỹ Thuật

### 3.1. Phân Hệ Giỏ Hàng Redis (Redis Cart Engine)
- **Tốc độ mili-giây**: Thao tác thêm, bớt, xóa sản phẩm trong giỏ thực thi trực tiếp trên cấu trúc **Redis HASH (`cart:user:{userId}`)**.
- **Shopee Multi-shop Grouping**: Dữ liệu giỏ hàng trả về phía Client được tự động phân tách và gom nhóm logic theo từng Shop (`CartShopGroupResponse`).
- **Gia hạn tự động (TTL 30 ngày)**: Khóa Redis được cấu hình TTL 30 ngày (2,592,000 giây) và tự động gia hạn (`EXPIRE`) mỗi khi giỏ hàng phát sinh tương tác.
- **Dọn dẹp nguyên tử**: Khi checkout thành công, hệ thống xóa chính xác các SKU đã đặt mà không ảnh hưởng tới các mặt hàng khác còn lại trong giỏ.

### 3.2. Kiểm Soát Tồn Kho Flash Sale & Chống Race Condition
- **Khóa bi quan (`PESSIMISTIC_WRITE`)**: Sử dụng cú pháp `SELECT ... FOR UPDATE` qua Spring Data JPA để khóa bản ghi SKU trước khi trừ kho.
- **Stress Concurrency Verified**: Đã vượt qua kiểm thử tải đồng thời **200 threads** tranh mua cùng một sản phẩm tồn kho giới hạn; triệt tiêu hoàn toàn tình trạng âm kho (Overselling) và Deadlock.
- **Cơ chế Hoàn kho tự động**: Khi đơn hàng chuyển trạng thái sang `CANCELLED` hoặc `RETURNED`, hệ thống kích hoạt transaction tự động hoàn trả số lượng tồn kho cho SKU và Product tương ứng.

### 3.3. Cổng Thanh Toán VNPAY & Webhook IPN Idempotent
- **Mã hóa an toàn**: Tạo chuỗi thanh toán VNPAY bảo mật với thuật toán băm chữ ký số **HMAC-SHA512**.
- **Xử lý bất đồng bộ qua Webhook IPN**: Ghi nhận kết quả thanh toán từ VNPAY qua endpoint `/api/v1/payments/vnpay-ipn`.
- **Tính toán đẳng năng (Idempotent)**: Ngăn ngừa xử lý trùng lặp giao dịch khi máy chủ thanh toán gửi retry nhiều lần, đảm bảo đơn hàng chỉ cập nhật trạng thái `PAID` duy nhất 1 lần.

### 3.4. Hàng Đợi Bất Đồng Bộ RabbitMQ & Chống Nghẽn Luồng
- **Tách biệt tác vụ nặng**: Luồng gửi email xác nhận đơn hàng, biên lai thanh toán được chuyển sang worker bất đồng bộ, giúp API phản hồi cho khách hàng ngay lập tức (< 100ms).
- **Chính sách Thử lại & DLQ**: Cấu hình Spring AMQP retry với khoảng lặp tăng dần (2s -> 4s -> 8s, tối đa 3 lần). Khi dịch vụ mail gặp sự cố, message được giữ an toàn tại Dead Letter Queue (`order.email.dlq`).

### 3.5. Trải Nghiệm Frontend Shopee Mall Toàn Diện (Ocean Blue)
- **Chuẩn phong cách Shopee**: Thiết kế giao diện theo phong cách Shopee Mall với bộ nhận diện màu **Ocean Blue (`#0284C7`)**.
- **Cây danh mục đệ quy**: Hiển thị sidebar danh mục đa cấp, tự động lọc cả danh mục cha lẫn các danh mục con cháu.
- **Trang chi tiết sản phẩm (PDP)**: Hỗ trợ biến thể SKU 2 chiều (Màu sắc, Kích thước), tự động đồng bộ giá bán, số lượng tồn kho thực tế và hình ảnh sản phẩm.
- **Sticky Checkout Bar**: Thanh tính tiền bám dính, hỗ trợ áp dụng mã giảm giá (Vouchers), tính toán phí vận chuyển và giảm giá linh hoạt.
- **6 Tab Quản lý Đơn hàng**: Quản lý đầy đủ vòng đời đơn hàng: *Tất cả, Chờ thanh toán, Đang xử lý, Đang vận chuyển, Hoàn thành, Đã hủy*.
- **Chống Race Condition Refresh Token**: Tích hợp hàng đợi `failedQueue` trong Axios Interceptor, ngăn chặn tình trạng bắn đồng loạt nhiều request refresh token khi Access Token hết hạn.
- **100% Real API Driven**: Toàn bộ dữ liệu hiển thị từ cơ sở dữ liệu MySQL thông qua hệ thống REST API.

### 3.6. Tối Ưu Hóa Hiệu Năng Truy Vấn & Caching
- **Trị dứt điểm bài toán N+1 Query**: Áp dụng `@EntityGraph(attributePaths = {"category", "shop", "skus", "images"})` giúp tải dữ liệu sản phẩm chỉ với 1 câu SQL `JOIN`.
- **Redis Cache-Aside**: Áp dụng `@Cacheable` cho dữ liệu đọc thường xuyên (chi tiết sản phẩm, cây danh mục) và `@CacheEvict` tự động xóa cache khi người bán cập nhật hàng hóa.

---

## 4. Ngăn Xếp Công Nghệ (Tech Stack)

| Lớp Kiến Trúc | Công Nghệ / Thư Viện | Phiên Bản | Mô Tả Vai Trò |
| :--- | :--- | :---: | :--- |
| **Backend Core** | Java (OpenJDK / Eclipse Temurin) | **21** | Record, Pattern Matching & Virtual Threads |
| **Framework** | Spring Boot | **3.3.5** | Spring MVC, Spring Data JPA, Spring AMQP, Spring Mail |
| **Security** | Spring Security & JJWT | **6.x / 0.12.6** | Phân quyền RBAC, Stateless JWT (15m), Redis Refresh (7d) |
| **Data Mapping** | MapStruct & Lombok | **1.5.5 / 1.18.34** | Sinh mã chuyển đổi DTO compile-time hiệu năng cao |
| **API Docs** | Springdoc OpenAPI | **2.6.0** | Tài liệu hóa OpenAPI 3.0 tự động kèm Swagger UI |
| **Frontend UI** | React | **18.3.1** | Xây dựng Single Page Application chuẩn component |
| **Build Tool** | Vite | **5.4.9** | Hot Module Replacement (HMR) và đóng gói bundle |
| **Ngôn Ngữ** | TypeScript | **5.6.3** | Đảm bảo tính chặt chẽ về kiểu dữ liệu |
| **Styling** | Tailwind CSS | **3.4.14** | Utility-first CSS, thiết kế responsive và theme Ocean Blue |
| **Quản Lý State** | Zustand | **5.0.0** | Quản lý state gọn nhẹ, đồng bộ LocalStorage |
| **Server State** | TanStack Query | **5.59.16** | Caching API requests, xử lý background refetch |
| **HTTP Client** | Axios | **1.7.7** | Request/Response interceptor với cơ chế failedQueue |
| **Cơ Sở Dữ Liệu** | MySQL Server | **8.0** | Lưu trữ quan hệ chính thống, InnoDB Engine, utf8mb4 |
| **Bộ Nhớ Đệm** | Redis Server | **7-alpine** | Giỏ hàng phân tán (Hash), Refresh Token, AOF Persistent |
| **Message Broker** | RabbitMQ | **3.13-management** | Hàng đợi sự kiện đơn hàng, DLQ, Exchange định tuyến |
| **Reverse Proxy** | Nginx | **1.27-alpine** | Định tuyến SPA, Gzip nén tĩnh, chuyển tiếp proxy `/api/` |
| **Containerization**| Docker & Docker Compose | **v2+** | Môi trường đóng gói Multi-stage build cho dev & prod |

---

## 5. Cấu Trúc Thư Mục Dự Án

```plaintext
WebMall/
├── backend/                            # Toàn bộ mã nguồn Spring Boot Core Backend
│   ├── src/main/java/com/helishop/core/
│   │   ├── HeliShopApplication.java   # Bootstrap application
│   │   ├── common/                    # Lớp dùng chung: BaseEntity, Exception, ApiResponse<T>
│   │   ├── config/                    # Cấu hình hệ thống (Security, Redis, RabbitMQ, Swagger)
│   │   ├── security/                  # Bộ lọc JWT (JwtFilter, JwtUtils, UserDetails)
│   │   └── modules/                   # 8 Phân hệ nghiệp vụ độc lập (Domain Modules)
│   │       ├── auth/                  # Đăng ký, Đăng nhập, Refresh Token, Phân quyền RBAC
│   │       ├── user/                  # Hồ sơ người dùng, Quản lý địa chỉ giao hàng, Gian hàng Shop
│   │       ├── product/               # Hàng hóa, Biến thể SKU, Cây danh mục, Lọc động JPA Spec
│   │       ├── cart/                  # Động cơ giỏ hàng Shopee trên Redis Hash (TTL 30 ngày)
│   │       ├── order/                 # Đơn hàng, Khóa bi quan trừ tồn kho, Hoàn kho khi hủy đơn
│   │       ├── payment/               # Cổng thanh toán VNPAY, Ký số HMAC-SHA512, Idempotent IPN
│   │       ├── notification/          # RabbitMQ Async Email Consumer, Retry x3, Dead Letter Queue
│   │       └── media/                 # Tải lên tệp đa phương tiện (Cloudinary / Local storage)
│   ├── src/main/resources/
│   │   ├── application.yml            # Cấu hình kết nối MySQL, Redis, RabbitMQ, Mail, JWT
│   │   └── seed-data.sql              # Kịch bản nạp dữ liệu mẫu ban đầu
│   ├── src/test/                      # 103 bài kiểm thử chuyên sâu (Unit, Integration, E2E, Stress)
│   ├── Dockerfile                     # Multi-stage Dockerfile cho Backend (Temurin 21 JRE)
│   └── pom.xml                        # Quản lý thư viện phụ thuộc Maven
│
├── frontend/                           # Toàn bộ mã nguồn ứng dụng React 18 SPA
│   ├── src/
│   │   ├── components/                # UI Components tái sử dụng (Header, Footer, ProductCard...)
│   │   ├── pages/                     # Màn hình ứng dụng (Home, PDP, Cart, Checkout, Orders...)
│   │   ├── store/                     # Zustand stores (useAuthStore, useCartStore...)
│   │   ├── lib/                       # Cấu hình Axios Interceptor (failedQueue silent refresh)
│   │   ├── types/                     # Định nghĩa TypeScript interfaces (Product, Cart, Order...)
│   │   ├── App.tsx                    # Định tuyến đường dẫn ứng dụng (Routing)
│   │   └── index.css                  # Cấu hình Tailwind CSS và bảng màu Ocean Blue
│   ├── nginx.conf                     # Cấu hình máy chủ Nginx reverse proxy cho production
│   ├── Dockerfile                     # Multi-stage Dockerfile cho Frontend (Node 20 -> Nginx)
│   ├── package.json                   # Khai báo dependencies NPM
│   └── vite.config.ts                 # Cấu hình build Vite & proxy development
│
├── docs/                               # Tài liệu thiết kế, Kiến trúc & Bộ nhớ dự án
│   ├── ARCHITECTURE.md                # Tài liệu chi tiết thiết kế kiến trúc hệ thống
│   ├── ROADMAP.md                     # Lộ trình hoàn thành các Sprint qua các giai đoạn
│   ├── MEMORY.md                      # Trạng thái hiện thời, thông số kỹ thuật & bài học
│   └── sprints/                       # Nhật ký chi tiết của từng Sprint từ 1 đến 6
│
├── docker-compose.yml                 # Môi trường hạ tầng phát triển Local (MySQL, Redis, RabbitMQ, Backend)
├── docker-compose.production.yml      # Cụm 5 dịch vụ khép kín Production (Frontend Nginx, Backend, DBs)
└── AGENTS.md                          # Bộ quy tắc làm việc cho AI Agents & Git Commit Policy
```

---

## 6. Hướng Dẫn Cài Đặt & Khởi Chạy Nhanh

Hệ thống hỗ trợ 2 phương thức khởi chạy: Triển khai toàn diện với Docker Production hoặc Chạy môi trường Phát triển Local (Development).

### 6.1. Triển Khai Trọn Gói Với Docker Compose Production (Khuyến nghị)

Toàn bộ 5 dịch vụ (`frontend`, `backend`, `mysql`, `redis`, `rabbitmq`) được đóng gói và khởi chạy qua một lệnh duy nhất:

```bash
docker compose -f docker-compose.production.yml up -d --build
```

Sau khi các container đạt trạng thái `healthy`:
- **Giao diện người dùng (Frontend)**: `http://localhost` (Port 80)
- **Tài liệu API tương tác (Swagger UI)**: `http://localhost:8080/swagger-ui/index.html`
- **Bảng điều khiển RabbitMQ**: `http://localhost:15672` (Tài khoản: `guest` / `guest`)

Dừng và dọn dẹp hệ thống:
```bash
docker compose -f docker-compose.production.yml down
```

---

### 6.2. Khởi Chạy Môi Trường Phát Triển Local (Development)

#### Yêu Cầu Môi Trường Cần Chuẩn Bị
- **Java**: OpenJDK / Temurin 21 trở lên
- **Node.js**: Phiên bản 18+ (khuyên dùng Node 20 LTS) & `npm`
- **Docker & Docker Compose**: Để khởi chạy các dịch vụ lưu trữ phụ trợ

---

#### Bước 1: Khởi động các dịch vụ phụ trợ (MySQL, Redis, RabbitMQ)
Chạy lệnh sau tại thư mục gốc của dự án:
```powershell
docker compose up -d mysql redis rabbitmq
```
*Lệnh này khởi động MySQL 8.0 (Port 3307), Redis 7 (Port 6379), RabbitMQ 3.13 (Port 5672/15672).*

---

#### Bước 2: Nạp dữ liệu mẫu (Seed Data)
Hệ thống cung cấp tệp SQL mẫu tại `backend/src/main/resources/seed-data.sql` chứa cấu trúc danh mục, gian hàng, sản phẩm biến thể và tồn kho khởi tạo:

```powershell
# Thực thi nạp dữ liệu trực tiếp vào container MySQL trên Windows PowerShell
Get-Content backend\src\main\resources\seed-data.sql | docker exec -i eshop_mysql mysql -ueshop_user -peshop_secret eshop_db
```
*(Trên hệ điều hành Linux/macOS: `cat backend/src/main/resources/seed-data.sql | docker exec -i eshop_mysql mysql -ueshop_user -peshop_secret eshop_db`)*

---

#### Bước 3: Khởi chạy Backend (Spring Boot Core)
Di chuyển vào thư mục `backend` và khởi động máy chủ API:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*Máy chủ Backend sẽ lắng nghe tại `http://localhost:8080`.*

---

#### Bước 4: Khởi chạy Frontend (React 18 + Vite)
Mở một cửa sổ dòng lệnh mới, di chuyển vào thư mục `frontend` và khởi động:
```powershell
cd frontend
npm install
npm run dev
```
*Ứng dụng giao diện sẽ sẵn sàng tại `http://localhost:5173` với cơ chế Hot Reload (HMR).*

---

## 7. Thông Số Cổng Dịch Vụ & Biến Môi Trường

### 7.1. Bảng Thông Số Cổng Dịch Vụ

| Dịch Vụ | Cổng (Port) | Địa Chỉ Truy Cập (URL) | Ghi Chú |
| :--- | :---: | :--- | :--- |
| **Frontend Web (Dev)** | `5173` | `http://localhost:5173` | Môi trường phát triển React 18 + Vite |
| **Frontend Web (Prod)** | `80` | `http://localhost` | Phục vụ qua Nginx Reverse Proxy |
| **Backend REST API** | `8080` | `http://localhost:8080` | Spring Boot 3 Core Server |
| **Swagger UI (OpenAPI 3)** | `8080` | `http://localhost:8080/swagger-ui/index.html` | Tài liệu API tương tác trực tiếp |
| **MySQL Server** | `3307` | `localhost:3307` (Host) -> `3306` | User: `eshop_user` (Cơ sở dữ liệu: `eshop_db`) |
| **Redis Server** | `6379` | `localhost:6379` | Port mặc định Redis 7 AOF |
| **RabbitMQ Management** | `15672` | `http://localhost:15672` | Giao diện quản trị hàng đợi |
| **RabbitMQ AMQP Broker** | `5672` | `localhost:5672` | Giao thức truyền tin nhị phân AMQP |

---

### 7.2. Các Biến Môi Trường Cấu Hình Trọng Điểm

Các tham số này có thể ghi đè thông qua biến môi trường hệ thống hoặc trong `docker-compose`:

```properties
# Kết nối cơ sở dữ liệu MySQL
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/eshop_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&createDatabaseIfNotExist=true&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=eshop_user
SPRING_DATASOURCE_PASSWORD=eshop_secret

# Kết nối Redis & RabbitMQ
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672

# Cổng thanh toán VNPAY Sandbox
VNP_TMN_CODE=HELI0001
VNP_HASH_SECRET=404E635266556A586E3272357538782F
VNP_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNP_RETURN_URL=http://localhost:8080/api/v1/payments/vnpay-return
VNP_IPN_URL=http://localhost:8080/api/v1/payments/vnpay-ipn
```

---

## 8. Kiểm Thử & Đảm Bảo Chất Lượng (QA)

Dự án sở hữu bộ kiểm thử tự động toàn diện gồm **103 bài kiểm thử (100% Passed)** trải dài từ tầng Unit Test, MockMvc Integration Test đến E2E Concurrency Stress Test:

<div align="center">

| Nhóm Kiểm Thử | Số Lượng Test | Mục Tiêu Đảm Bảo |
| :--- | :---: | :--- |
| **Concurrency Stress Test** | 2 tests | Kiểm thử tải 200 threads đồng thời với `CountDownLatch`: Chống âm kho & Race Condition |
| **E2E Payment & DLQ** | 2 tests | Giả lập Webhook VNPAY chữ ký số, Idempotent và chuyển tin sang Dead Letter Queue khi SMTP sự cố |
| **Pessimistic Locking** | 4 tests | Khóa bi quan `PESSIMISTIC_WRITE`, trừ tồn kho nguyên tử & kiểm tra ngoại lệ `InsufficientStockException` |
| **Order Status & Return** | 3 tests | Kiểm tra hoàn trả tồn kho tự động khi trạng thái chuyển sang `CANCELLED` hoặc `RETURNED` |
| **Redis Cart Engine** | 10 tests | Thêm, sửa, xóa, dọn giỏ, cấu trúc gom nhóm Shop và gia hạn TTL 30 ngày trên Redis HASH |
| **Redis Cache-Aside** | 4 tests | Đảm bảo tính nhất quán của `@Cacheable` và `@CacheEvict` trên danh mục và sản phẩm |
| **Security & JWT Auth** | 18 tests | Xác thực Bearer JWT, phân quyền chi tiết RBAC và kiểm thử Silent Refresh Token |
| **Catalog & Dynamic Query**| 12 tests | Kiểm thử cây danh mục đệ quy, JPA Specification đa tiêu chí và xử lý triệt để N+1 query |
| **Unit & Validation Tests** | 48 tests | Kiểm thử tầng DTO Bean Validation (`@Valid`), MapStruct Mappers và Exception Handler |

</div>

### Lệnh Thực Thi Các Bài Kiểm Thử Trọng Điểm

```powershell
# Di chuyển vào thư mục backend
cd backend

# 1. Chạy bài kiểm thử Đa luồng 200 Threads Chống âm kho (Stress Test)
.\mvnw.cmd test "-Dtest=CartAndStockConcurrencyStressTest"

# 2. Chạy bài kiểm thử E2E Thanh toán VNPAY & Hàng đợi chịu lỗi DLQ
.\mvnw.cmd test "-Dtest=PaymentAndEmailDlqE2ETest"

# 3. Chạy kiểm thử Khóa bi quan chống Race Condition
.\mvnw.cmd test "-Dtest=PessimisticLockingIntegrationTest"

# 4. Chạy kiểm thử Động cơ giỏ hàng Redis
.\mvnw.cmd test "-Dtest=CartServiceIntegrationTest"

# 5. Chạy toàn bộ 103 bài kiểm thử của hệ sinh thái Backend
.\mvnw.cmd test
```

---

## 9. Tài Liệu API (OpenAPI 3.0 / Swagger)

Hệ thống được tài liệu hóa 100% các Endpoints, DTO Schemas và Mã phản hồi HTTP theo chuẩn **OpenAPI 3.0**.

- **Giao diện trực quan Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **Đặc tả JSON OpenAPI**: `http://localhost:8080/v3/api-docs`

```plaintext
Cấu trúc phản hồi JSON chuẩn mực (ApiResponse<T>):
{
  "code": 200,
  "message": "Thông điệp phản hồi chi tiết",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-09-11T09:00:00"
}
```

### Các Nhóm Endpoint Chính:
1. **`POST /api/v1/auth/*`**: Đăng ký tài khoản, đăng nhập bằng Email/SĐT, refresh token và lấy thông tin cá nhân (`/me`).
2. **`GET /api/v1/products`**: Tra cứu và lọc sản phẩm động đa tiêu chí (khoảng giá, danh mục con, rating sao, phân trang).
3. **`GET /api/v1/categories/tree`**: Trả về cấu trúc cây danh mục đa cấp (hỗ trợ cache Redis).
4. **`GET /api/v1/cart`, `POST /api/v1/cart/items`**: Thao tác giỏ hàng Redis, cập nhật số lượng và gom nhóm theo Shop.
5. **`POST /api/v1/orders/checkout`**: Tiến hành đặt hàng nguyên tử có khóa bi quan chống âm kho.
6. **`GET /api/v1/orders`, `PATCH /api/v1/orders/{id}/status`**: Quản lý lịch sử đơn hàng, cập nhật trạng thái và hoàn kho.
7. **`POST /api/v1/payments/vnpay-ipn`**: Nhận Webhook xác nhận thanh toán tự động từ máy chủ VNPAY.

---

## 10. Lộ Trình Phát Triển (Roadmap)

```mermaid
gantt
    title LO TRINH TRIEN KHAI DU AN HELISHOP
    dateFormat  YYYY-MM-DD
    section Project 1: Backend Platform
    Sprint 1-3 (Ha tang, Auth & Catalog)    :done, p1_1, 2026-08-01, 2026-08-15
    Sprint 4-6 (Locking, Cache & Docker)    :done, p1_2, 2026-08-16, 2026-08-31
    section Project 2: Shopee E-Commerce
    Sprint 1 (Redis Cart & RabbitMQ DLQ)    :done, p2_1, 2026-09-01, 2026-09-04
    Sprint 2 (VNPAY Sandbox & Webhook IPN)  :done, p2_2, 2026-09-05, 2026-09-07
    Sprint 3 (Async Email & Media Upload)   :done, p2_3, 2026-09-08, 2026-09-09
    Sprint 4 (Frontend Core & Interceptor)  :done, p2_4, 2026-09-09, 2026-09-10
    Sprint 5 (Shopee UI & Ocean Blue Theme) :done, p2_5, 2026-09-10, 2026-09-10
    Sprint 6 (E2E Stress Test & Production) :done, p2_6, 2026-09-11, 2026-09-11
    section Project 3: Cloud & Microservices
    Spring Cloud Gateway & Discovery        :active, p3_1, 2026-09-15, 2026-09-30
    ElasticSearch Search & Analytics Engine :p3_2, 2026-10-01, 2026-10-15
    Kubernetes (K8s) Cluster Deployment     :p3_3, 2026-10-16, 2026-10-31
```

- **Project 1: Core E-Commerce Backend Platform**: [Hoàn thành 100% - 6/6 Sprints]
- **Project 2: Shopee E-Commerce Integration & Automation**: [Hoàn thành 100% - 6/6 Sprints]
- **Project 3: Production Hardening, Cloud Microservices & Kubernetes**: [Kế hoạch mở rộng tương lai]

---

## 11. Bản Quyền & Giấy Phép (License)

Dự án được phân phối dưới giấy phép **MIT License**. Mọi cá nhân, tổ chức được tự do sử dụng, nghiên cứu, mở rộng và tích hợp vào các dự án thương mại hoặc học tập.

<div align="center">
  <sub>Phát triển bởi <b>HeliShop Engineering Team</b></sub>
</div>
