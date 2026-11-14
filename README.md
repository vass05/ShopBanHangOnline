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
[![Tests](https://img.shields.io/badge/Tests-152_Backend-brightgreen?style=flat-square&logo=junit5&logoColor=white)]()

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

## Cấu Trúc Dự Án Chi Tiết (Project Structure)

```plaintext
WebMall/
├── backend/                                   # Mã nguồn Backend (Spring Boot 3.3.5 / Java 21)
│   ├── src/main/java/com/helishop/core/
│   │   ├── HeliShopApplication.java          # Điểm khởi chạy ứng dụng Spring Boot
│   │   ├── common/                           # Thành phần dùng chung toàn hệ thống
│   │   │   ├── entity/BaseEntity.java        # Thực thể cơ sở (id, createdAt, updatedAt)
│   │   │   ├── exception/AppException.java   # Xử lý ngoại lệ tập trung & ErrorCode
│   │   │   └── response/ApiResponse.java     # Chuẩn hóa cấu trúc phản hồi JSON chuẩn
│   │   ├── config/                           # Cấu hình hạt nhân hệ thống
│   │   │   ├── SecurityConfig.java           # Spring Security 6 & CORS
│   │   │   ├── RedisConfig.java              # Cấu hình RedisTemplate & CacheManager
│   │   │   ├── RabbitMqConfig.java           # Định nghĩa Exchange, Queue, DLQ
│   │   │   └── OpenApiConfig.java            # Cấu hình tài liệu Swagger UI / OpenAPI 3.0
│   │   ├── security/                         # Bảo mật phân quyền & JWT
│   │   │   ├── JwtFilter.java                # Bộ lọc chặn request để giải mã Bearer JWT
│   │   │   ├── JwtUtils.java                 # Tiện ích sinh & kiểm tra tính hợp lệ của Token
│   │   │   └── CustomUserDetailsService.java # Nạp thông tin người dùng xác thực
│   │   └── modules/                          # 8 Phân hệ nghiệp vụ độc lập (Domain Modules)
│   │       ├── auth/                         # Đăng ký, đăng nhập & JWT Refresh Token
│   │       │   ├── controller/AuthController.java
│   │       │   └── service/AuthService.java, RefreshTokenService.java
│   │       ├── cart/                         # Động cơ giỏ hàng Shopee trên Redis
│   │       │   ├── controller/CartController.java
│   │       │   ├── dto/CartItemDto.java, CartShopGroupResponse.java
│   │       │   └── service/RedisCartService.java (Thao tác Redis Hash & gia hạn TTL 30 ngày)
│   │       ├── product/                      # Quản lý hàng hóa, biến thể SKU & danh mục
│   │       │   ├── controller/ProductController.java, CategoryController.java
│   │       │   ├── entity/Product.java, ProductSku.java, Category.java
│   │       │   ├── repository/ProductSkuRepository.java (findByIdWithLock - Khóa bi quan)
│   │       │   └── service/ProductService.java, CategoryService.java
│   │       ├── order/                        # Đơn hàng, khóa trừ tồn kho & hoàn kho
│   │       │   ├── controller/OrderController.java
│   │       │   ├── entity/Order.java, OrderItem.java
│   │       │   ├── repository/OrderRepository.java
│   │       │   └── service/OrderService.java (Khóa bi quan trừ kho & hoàn kho tự động)
│   │       ├── payment/                      # Cổng thanh toán VNPAY & Webhook IPN
│   │       │   ├── controller/PaymentController.java
│   │       │   ├── service/PaymentService.java (Tạo URL VNPAY & xác thực chữ ký SHA512)
│   │       │   └── event/OrderEventPublisher.java (Bắn sự kiện OrderPaidEvent sang RabbitMQ)
│   │       ├── notification/                 # Xử lý hàng đợi bất đồng bộ & Email
│   │       │   ├── consumer/OrderEmailConsumer.java (Tiêu thụ tin nhắn gửi mail)
│   │       │   ├── consumer/OrderDlqConsumer.java (Lưu trữ và giám sát Dead Letter Queue)
│   │       │   └── service/EmailService.java (Tạo nội dung HTML MimeMessage)
│   │       ├── user/                         # Người dùng, địa chỉ giao hàng & gian hàng Shop
│   │       │   ├── controller/UserController.java
│   │       │   └── entity/User.java, UserAddress.java, Shop.java
│   │       └── media/                        # Quản lý tải lên tệp đa phương tiện
│   │           ├── controller/MediaController.java
│   │           └── service/MediaService.java
│   ├── src/main/resources/
│   │   ├── application.yml                   # Cấu hình MySQL, Redis, RabbitMQ, Mail, JWT
│   │   └── seed-data.sql                     # Kịch bản nạp dữ liệu mẫu ban đầu
│   ├── src/test/                             # Kiểm thử backend tự động
│   │   ├── CartAndStockConcurrencyStressTest # Stress test 200 threads chống âm kho
│   │   ├── PaymentAndEmailDlqE2ETest.java    # E2E Webhook VNPAY & cơ chế chịu lỗi DLQ
│   │   ├── PessimisticLockingIntegrationTest # Kiểm thử khóa bi quan dòng PESSIMISTIC_WRITE
│   │   └── CartServiceIntegrationTest.java   # Kiểm thử động cơ giỏ hàng Redis Hash
│   ├── Dockerfile                            # Đóng gói container Backend (Eclipse Temurin 21 JRE)
│   └── pom.xml                               # Quản lý thư viện phụ thuộc Maven
│
├── frontend/                                  # Giao diện người dùng Shopee Mall (React 18 + Vite)
│   ├── src/
│   │   ├── App.tsx                           # Khai báo tuyến đường ứng dụng (Routing)
│   │   ├── index.css                         # Cấu hình Tailwind CSS & theme Ocean Blue (#0284C7)
│   │   ├── components/                       # UI Components tái sử dụng
│   │   │   ├── catalog/ProductCard.tsx       # Thẻ sản phẩm chuẩn Shopee (giá, ảnh, đã bán)
│   │   │   ├── catalog/SidebarFilter.tsx     # Bộ lọc cây danh mục, khoảng giá, rating sao
│   │   │   ├── layout/Header.tsx, Footer.tsx # Thanh tìm kiếm, điều hướng, giỏ hàng nổi
│   │   │   └── ui/button.tsx, card.tsx...    # Thư viện component UI cơ bản
│   │   ├── pages/                            # Các màn hình ứng dụng chính
│   │   │   ├── HomePage.tsx                  # Trang chủ: Banner, Flash Sale, danh mục, gợi ý
│   │   │   ├── ProductDetailPage.tsx         # Trang PDP: SKU biến thể 2 chiều, ảnh, tồn kho
│   │   │   ├── CartPage.tsx                  # Giỏ hàng gom nhóm theo Shop, chọn mua linh hoạt
│   │   │   ├── CheckoutPage.tsx              # Thanh toán: chọn địa chỉ, COD/VNPAY
│   │   │   ├── OrdersPage.tsx                # Quản lý đơn hàng: 6 tab trạng thái vòng đời
│   │   │   └── PaymentResultPage.tsx         # Trang hiển thị kết quả giao dịch từ VNPAY
│   │   ├── store/                            # Quản lý State toàn cục (Zustand)
│   │   │   ├── useAuthStore.ts               # Quản lý phiên đăng nhập & thông tin User
│   │   │   └── useCartStore.ts               # Đồng bộ số lượng & trạng thái giỏ hàng
│   │   ├── lib/
│   │   │   ├── api.ts                        # Axios Interceptor (Silent Refresh Token với failedQueue)
│   │   │   └── formatters.ts                 # Định dạng tiền tệ VNĐ, ngày tháng
│   │   └── types/index.ts                    # Khai báo TypeScript interfaces (Product, Order, Cart...)
│   ├── nginx.conf                            # Nginx reverse proxy cho Production (SPA routing & /api/)
│   ├── Dockerfile                            # Multi-stage Dockerfile cho Frontend (Node 20 -> Nginx)
│   └── package.json                          # Khai báo các thư viện phụ thuộc NPM
│
├── docs/                                      # Bộ tài liệu thiết kế, kiến trúc & quản lý dự án
│   ├── ARCHITECTURE.md                       # Thiết kế kiến trúc, sơ đồ Sequence & luồng dữ liệu
│   ├── ROADMAP.md                            # Lộ trình hoàn thiện các Sprint từ 1 đến 6
│   ├── MEMORY.md                             # Trạng thái kỹ thuật hệ thống & bài học kinh nghiệm
│   └── sprints/                              # Nhật ký triển khai chi tiết từng Sprint
│
├── docker-compose.yml                        # Môi trường hỗ trợ phát triển Local (MySQL, Redis, RabbitMQ)
├── docker-compose.production.yml             # Cụm triển khai Production 5 container khép kín
└── AGENTS.md                                 # Quy tắc phát triển & quy chuẩn Git Commit
```

---

## Hướng Dẫn Khởi Chạy (Quick Start)

### Cách 1: Chạy toàn bộ bằng Docker

Lần đầu, bật Docker Desktop và tạo file cấu hình riêng (mật khẩu được sinh ngẫu nhiên, không commit):

```powershell
.\scripts\Initialize-Environment.ps1
docker compose --env-file .env.production -f docker-compose.production.yml up -d --build --wait
docker compose --env-file .env.production -f docker-compose.production.yml ps
```

Frontend: http://localhost — API và ảnh `/uploads` đi qua cùng Nginx. Swagger: http://localhost/swagger-ui/index.html. Có thể truy cập giao diện bằng IP máy chủ từ máy khác. Đổi `FRONTEND_PORT` và URL callback trong `.env.production` nếu cần.

Những lần sau, bật Docker Desktop rồi chạy lại lệnh `up -d --wait`; thêm `--build` khi có thay đổi mã nguồn. `restart: unless-stopped` giúp container tự bật lại nếu trước đó chưa bị dừng thủ công. Chỉ bật Docker Desktop chưa bảo đảm cụm đã được tạo và đang healthy.

```powershell
# Dừng, giữ dữ liệu và ảnh upload
docker compose --env-file .env.production -f docker-compose.production.yml down
# Xem lỗi khởi động
docker compose --env-file .env.production -f docker-compose.production.yml logs --tail 100 backend frontend
```

MySQL/Redis/RabbitMQ production dùng cổng riêng `13307/16379/15673/15674` trong file mẫu, tránh xung đột với môi trường dev. Backend và các cổng quản trị chỉ bind localhost; Nginx mở cổng web. Các named volume giữ MySQL, Redis, RabbitMQ và ảnh đại diện qua lần tạo lại container. Giữ nguyên mật khẩu khi dùng lại volume cũ; thay mật khẩu trong file không tự đổi tài khoản đã có trong database/broker.

Database mới ban đầu chưa có hàng hóa. Chỉ nạp dữ liệu demo vào database thử nghiệm trống; `seed-data.sql` có cập nhật tài khoản và giá cố định, không chạy trên dữ liệu người dùng thật:

```powershell
Get-Content -Raw -Encoding UTF8 backend/src/main/resources/seed-data.sql | docker compose --env-file .env.production -f docker-compose.production.yml exec -T mysql sh -c 'exec mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
```

Cấu hình SMTP thật trước khi dùng OTP/email. VNPAY mặc định bị khóa (`VNP_DISABLED=true`): chỉ chuyển thành `false` sau khi đặt mã merchant, hash secret và các callback HTTPS công khai; Return trỏ `/payment-result`, IPN trỏ `/api/v1/payments/vnpay-ipn`. Chưa có merchant thì dùng COD. Upload ảnh sản phẩm qua Cloudinary cần cấu hình ba biến Cloudinary. Ảnh đại diện lưu trong volume cục bộ, tối đa 5 MB JPEG/PNG/WEBP.

`VITE_API_BASE_URL` là cấu hình **lúc build** (mặc định `/api/v1`), đổi biến này cần build lại frontend. Khi đưa ra Internet, đặt HTTPS/reverse proxy và hostname thực tế; cấu hình trên là cụm tự vận hành cục bộ, chưa thay thế quy trình migration/backup production.

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
npm ci
npm run dev
```
*(Frontend sẵn sàng tại `http://localhost:5173`)*

---

### Java trong VS Code

Dùng Java 21 và Red Hat Java. Backend giữ lifecycle mapping mặc định của m2e để IDE nhận đúng nguồn/classpath; không bỏ qua `maven-compiler-plugin` trong cấu hình IDE. Nếu Problems giữ lỗi import cũ, xem [hướng dẫn khôi phục Java IDE](docs/java-ide-troubleshooting.md).

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

Kiểm tra backend bằng Java 21 và frontend bằng Node 20 trở lên:

```powershell
cd backend
.\mvnw.cmd clean test
cd ../frontend
npm ci
npm test
npm run build
```

Browser E2E sử dụng cụm Docker riêng, hai shop/SKU thử nghiệm và Mailpit thu email nội bộ. Không gửi email ra Internet hoặc giao dịch với ngân hàng. Từ thư mục gốc:

```powershell
.\scripts\Prepare-Integration.ps1
cd frontend
npx playwright install chromium
npm run test:e2e
```

Trên Windows đã có Chrome, có thể đặt `$env:E2E_BROWSER_CHANNEL='chrome'` thay bước tải Chromium. Web thử: http://localhost:18000 ; hộp thư nội bộ: http://localhost:18025. Script chỉ nhận project/volume `helishop-integration` đã định trước, không nạp fixture vào dev/production. Chạy lại script sẽ đặt tồn kho hai SKU thử về 50.

Các ca kiểm tra gồm mua COD, Redis TTL, quyền sở hữu đơn, hủy/hoàn kho, lưu phiên và refresh/logout, OTP qua SMTP, checkout nhiều shop chống trùng, Return chờ IPN, IPN đồng thời, outbox/RabbitMQ/email, avatar qua tạo lại backend và SMTP lỗi vào DLQ. Trong ca cuối, Mailpit của cụm thử được dừng rồi bật lại; thông điệp lỗi giữ trong `order.email.dlq` để kiểm tra/gửi lại có kiểm soát. Listener DLQ không tự tiêu thụ chỉ để ghi log. Email giao nhận theo cơ chế at least once, vẫn có thể trùng khi worker dừng sau khi SMTP đã nhận thư nhưng trước khi ACK.

GitHub Actions chạy backend tests, frontend tests/build và toàn bộ cụm Docker/browser E2E cho PR và main, giữ báo cáo 7 ngày. Kết quả nghiệm thu và giới hạn nhà cung cấp nằm trong [bàn giao tích hợp](docs/sprints/integration-10-browser-ci.md).

---
## Tài Liệu Tham Khảo

- [Kiến Trúc Hệ Thống & Sơ Đồ Luồng (Architecture)](docs/ARCHITECTURE.md)
- [Lộ Trình Phát Triển & Trạng Thái Sprint (Roadmap)](docs/ROADMAP.md)
- [Nhật Ký Kỹ Thuật & Bài Học Kinh Nghiệm (Memory)](docs/MEMORY.md)

---

## Bản Quyền (License)

Dự án được phân phối theo giấy phép [MIT License](LICENSE).
