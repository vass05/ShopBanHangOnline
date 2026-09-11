# HeliShop - Enterprise E-Commerce Platform

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/)
[![React](https://img.shields.io/badge/React-18.3-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=flat-square&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-103%2F103_Passed-brightgreen?style=flat-square&logo=junit5&logoColor=white)]()

**Hệ thống Thương mại Điện tử Đa Nhà bán hàng (Multi-vendor Shopee Mall) chuẩn Doanh nghiệp.**  
*Tối ưu xử lý tải cao, giỏ hàng phân tán tốc độ cao, chống âm kho Flash Sale và tích hợp thanh toán trực tuyến.*

</div>

---

## Điểm Sáng Kỹ Thuật (Key Highlights)

- **Redis Cart Engine**: Giỏ hàng lưu trữ trên Redis Hash (`cart:user:{id}`), TTL tự động 30 ngày, gom nhóm đa gian hàng (Shopee Multi-shop).
- **Chống Âm Kho Flash Sale**: Khóa bi quan `PESSIMISTIC_WRITE` (Row-level lock) trên từng SKU, triệt tiêu race condition (đã kiểm thử chịu tải 200 threads).
- **Xử Lý Bất Đồng Bộ & DLQ**: RabbitMQ 3.13 gửi email nền, cơ chế retry x3 và chuyển tiếp an toàn vào Dead Letter Queue (`order.email.dlq`).
- **Thanh Toán VNPAY Idempotent**: Tích hợp VNPAY Sandbox HMAC-SHA512, Webhook IPN chuẩn Idempotent chống xử lý lặp đơn hàng.
- **Frontend Shopee Mall UI**: React 18, Vite, Tailwind CSS (theme Ocean Blue), Zustand, TanStack Query, Axios Silent Refresh Token.
- **Tối Ưu Truy Vấn & Cache**: Triệt tiêu lỗi N+1 Query với `@EntityGraph`, áp dụng Redis Cache-Aside cho danh mục và sản phẩm.

---

## Ngăn Xếp Công Nghệ (Tech Stack)

- **Backend**: Java 21, Spring Boot 3.3.5 (Web, Data JPA, Security, AMQP, Mail), MapStruct, Lombok.
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, TanStack Query, Zustand, Axios.
- **Database & Cache**: MySQL 8.0 (ACID, Row Locks), Redis 7 (Hash Cart & Cache AOF).
- **Message Broker**: RabbitMQ 3.13 (Direct Exchange & DLQ).
- **DevOps & Gateway**: Docker, Docker Compose Multi-stage, Nginx Reverse Proxy.

---

## Cấu Trúc Dự Án (Project Structure)

```plaintext
WebMall/
├── backend/                            # Spring Boot 3.3.5 Core Backend
│   ├── src/main/java/com/helishop/core/
│   │   ├── common/                    # BaseEntity, ApiResponse<T>, Global Exception Handler
│   │   ├── config/                    # Security, Redis, RabbitMQ, OpenAPI Swagger configs
│   │   ├── security/                  # JWT Filter, UserDetails, Token Provider
│   │   └── modules/                   # 8 Phân hệ nghiệp vụ độc lập (Domain Modules)
│   │       ├── auth/                  # Đăng ký, đăng nhập, JWT Refresh Token
│   │       ├── user/                  # Quản lý hồ sơ người dùng, địa chỉ giao hàng, Shop
│   │       ├── product/               # Quản lý hàng hóa, biến thể SKU, danh mục đa cấp
│   │       ├── cart/                  # Giỏ hàng Redis Hash Map, Shopee Multi-shop grouping
│   │       ├── order/                 # Đơn hàng, khóa bi quan trừ tồn kho, hoàn kho tự động
│   │       ├── payment/               # Cổng VNPAY Sandbox, chữ ký HMAC-SHA512, Webhook IPN
│   │       ├── notification/          # RabbitMQ Email Consumer, Retry x3, Dead Letter Queue
│   │       └── media/                 # Tải lên tệp đa phương tiện (Cloudinary / Local)
│   ├── src/main/resources/
│   │   ├── application.yml            # Cấu hình kết nối MySQL, Redis, RabbitMQ, Mail
│   │   └── seed-data.sql              # Kịch bản nạp dữ liệu mẫu ban đầu
│   ├── src/test/                      # 103 bài kiểm thử tự động (Unit, Integration, Stress, E2E)
│   ├── Dockerfile                     # Multi-stage Dockerfile cho Backend (Java 21 JRE)
│   └── pom.xml                        # Quản lý thư viện phụ thuộc Maven
│
├── frontend/                           # React 18 + Vite + TypeScript SPA
│   ├── src/
│   │   ├── components/                # UI Components tái sử dụng (Header, Footer, ProductCard...)
│   │   ├── pages/                     # Màn hình ứng dụng (Home, PDP, Cart, Checkout, Orders)
│   │   ├── store/                     # Quản lý State toàn cục với Zustand (Auth, Cart)
│   │   ├── lib/                       # Axios Interceptor (Silent Refresh Token với failedQueue)
│   │   ├── types/                     # Định nghĩa TypeScript interfaces cho Domain Models
│   │   └── index.css                  # Cấu hình Tailwind CSS và bảng màu Ocean Blue
│   ├── nginx.conf                     # Cấu hình Nginx Reverse Proxy cho môi trường Production
│   ├── Dockerfile                     # Multi-stage Dockerfile cho Frontend (Node 20 -> Nginx)
│   └── package.json                   # Khai báo các thư viện NPM
│
├── docs/                               # Tài liệu thiết kế, kiến trúc & quản lý dự án
│   ├── ARCHITECTURE.md                # Thiết kế kiến trúc chi tiết, Sơ đồ Sequence, Luồng checkout
│   ├── ROADMAP.md                     # Lộ trình và tiến độ qua từng giai đoạn Sprint
│   └── MEMORY.md                      # Trạng thái kỹ thuật hệ thống và bài học kinh nghiệm
│
├── docker-compose.yml                 # Cấu hình hạ tầng phát triển Local (MySQL, Redis, RabbitMQ)
└── docker-compose.production.yml      # Cụm triển khai Production 5 container khép kín
```

---

## Hướng Dẫn Khởi Chạy (Quick Start)

### Cách 1: Triển khai 1 lệnh với Docker Compose (Khuyên Dùng)

Khởi chạy trọn gói 5 dịch vụ (Frontend, Backend, MySQL, Redis, RabbitMQ):

```bash
docker compose -f docker-compose.production.yml up -d --build
```

- **Giao diện Web**: http://localhost
- **Swagger UI API**: http://localhost:8080/swagger-ui/index.html
- **RabbitMQ Dashboard**: http://localhost:15672 (`guest` / `guest`)

*Dừng hệ thống: `docker compose -f docker-compose.production.yml down`*

---

### Cách 2: Chạy Môi Trường Phát Triển (Local Dev)

**1. Khởi động MySQL, Redis, RabbitMQ:**
```powershell
docker compose up -d mysql redis rabbitmq
```

**2. Nạp dữ liệu mẫu ban đầu (Seed Data):**
```powershell
Get-Content backend\src\main\resources\seed-data.sql | docker exec -i eshop_mysql mysql -ueshop_user -peshop_secret eshop_db
```

**3. Khởi chạy Backend:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*(Backend sẵn sàng tại `http://localhost:8080`)*

**4. Khởi chạy Frontend:**
```powershell
cd frontend
npm install
npm run dev
```
*(Frontend sẵn sàng tại `http://localhost:5173`)*

---

## Cổng Dịch Vụ (Service Ports)

| Dịch Vụ | Port | URL / Địa Chỉ | Ghi Chú |
| :--- | :---: | :--- | :--- |
| **Frontend (Dev / Prod)** | `5173` / `80` | `http://localhost:5173` hoặc `http://localhost` | Giao diện người dùng Shopee Mall |
| **Backend REST API** | `8080` | `http://localhost:8080` | Spring Boot API Server |
| **Swagger UI (OpenAPI 3)** | `8080` | `http://localhost:8080/swagger-ui/index.html` | Tài liệu API tương tác trực tiếp |
| **MySQL Server** | `3307` | `localhost:3307` | Database: `eshop_db` (User: `eshop_user`) |
| **Redis Server** | `6379` | `localhost:6379` | Cache & Giỏ hàng phân tán Hash |
| **RabbitMQ Management** | `15672` | `http://localhost:15672` | Bảng điều khiển quản trị hàng đợi |

---

## Kiểm Thử Hệ Thống (Testing)

Dự án sở hữu bộ kiểm thử tự động toàn diện gồm **103 bài kiểm thử (100% Passed)**:

```powershell
cd backend

# Chạy toàn bộ 103 bài kiểm thử (Unit, Integration, Stress, E2E)
.\mvnw.cmd test

# Chạy kiểm thử tải 200 threads đồng thời chống âm kho
.\mvnw.cmd test "-Dtest=CartAndStockConcurrencyStressTest"
```

---

## Tài Liệu Tham Khảo

- [Kiến Trúc Hệ Thống & Sơ Đồ Luồng (Architecture)](docs/ARCHITECTURE.md)
- [Lộ Trình Phát Triển & Trạng Thái Sprint (Roadmap)](docs/ROADMAP.md)
- [Nhật Ký Kỹ Thuật & Bài Học Kinh Nghiệm (Memory)](docs/MEMORY.md)

---

## Bản Quyền (License)

Dự án được phân phối theo giấy phép [MIT License](LICENSE).
