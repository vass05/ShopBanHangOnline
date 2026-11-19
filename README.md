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
[![Tests](https://img.shields.io/badge/Tests-224_Backend-brightgreen?style=flat-square&logo=junit5&logoColor=white)]()

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
- **Mở Gian Hàng**: Đăng ký tài khoản người bán, mở và xem shop đã lưu tại `/seller/shop`; một shop cho mỗi seller, kiểm tra quyền sở hữu và chống tạo trùng đồng thời.

## Đăng nhập Google và tài khoản admin

**Tiếp tục với Google** là cách đăng ký/đăng nhập chính: Google xác minh Gmail, HeliShop không gửi OTP riêng. Người mới chọn người mua/người bán sau xác minh. Tài khoản email cũ xác nhận mật khẩu một lần để liên kết và giữ toàn bộ dữ liệu; lần sau chỉ bấm Google. Tài khoản cũ vẫn đăng nhập bằng mật khẩu được.

Điền Web Client ID vào `.env.google.local`, khai báo origins localhost/localhost:5173 tại Google Cloud, rồi chạy `Start-Local.ps1 -NoBuild`. Hướng dẫn từng bước: [thiết lập Google Sign-In](docs/google-sign-in-setup.md). Chưa có Client ID thì Google tạm chưa khả dụng; backend không giả lập xác thực.

Admin local có sẵn: `admin.helishop@gmail.com` / `Password123!` (nếu chưa đổi mật khẩu). Đăng nhập bằng form mật khẩu để vào `/admin/users`; không cần sở hữu Gmail mẫu này. Không tạo quyền admin qua đăng ký công khai. Số điện thoại chỉ dùng trong địa chỉ nhận hàng.

## Sử dụng chức năng mở gian hàng

Sau khi chạy frontend và backend theo hướng dẫn bên dưới, chọn **Kênh Người Bán** trên header hoặc mở `/register?role=seller`. Đăng ký tài khoản người bán, nhập tên gian hàng (3–150 ký tự, chưa được sử dụng) và mô tả tùy chọn (tối đa 2000 ký tự), rồi bấm **Mở gian hàng**. Trang `/seller/shop` hiển thị shop đã lưu; tải lại trang hoặc đăng nhập lại bằng tài khoản SELLER vẫn trở về shop đó.

Người bán đã có tài khoản chỉ cần đăng nhập. Shop mới hoạt động ngay, chưa có bước admin duyệt; tài khoản CUSTOMER không tự chuyển thành SELLER. Đã có quản lý sản phẩm/SKU và xử lý đơn bán bên dưới; dashboard sẽ triển khai trong chức năng riêng khi được yêu cầu.

## Quản lý sản phẩm và SKU của người bán

Từ **Gian hàng của tôi**, chọn **Quản lý sản phẩm & SKU**, hoặc vào `/seller/products`. Danh sách riêng shop có tìm kiếm, lọc trạng thái và phân trang. Chọn **Thêm sản phẩm**, điền thông tin/danh mục, URL ảnh tùy chọn và SKU đầu tiên; mặc định **Bản nháp**. Đổi thành **Đang bán** để đưa lên catalog, hoặc **Đã ẩn** để ngừng bán.

Trong màn chi tiết, thêm hoặc sửa SKU với mã, giá bán/giá gốc, tồn kho, URL ảnh và tối đa hai thuộc tính. Các SKU cùng sản phẩm dùng cùng tên thuộc tính, khác tổ hợp giá trị. Giá sản phẩm là giá SKU thấp nhất, tổng kho bằng tổng kho SKU. Nếu vừa có người mua hoặc người khác sửa SKU, hệ thống trả xung đột: chọn **Tải lại dữ liệu** rồi sửa trên số liệu mới nhất.

Ảnh hiện nhận URL http/https hoặc đường dẫn `/uploads/`; chưa có upload ảnh sản phẩm trong đợt này. SKU được giữ để bảo toàn tham chiếu đơn hàng; dùng trạng thái sản phẩm để ẩn bán. Chi tiết: [seller-product-sku-management.md](docs/sprints/seller-product-sku-management.md).

## Seller xử lý đơn bán

Từ **Gian hàng của tôi**, chọn **Xử lý đơn bán**, hoặc vào `/seller/orders`. Tìm mã đơn, lọc trạng thái/thanh toán rồi mở chi tiết để xem hàng, giá lúc mua, thông tin người nhận và ghi chú. Các nút hợp lệ do server xác định theo dữ liệu mới nhất:

**Xác nhận đơn → Bắt đầu đóng gói → Bàn giao vận chuyển → Đánh dấu đã giao**.

VNPAY cần được xác nhận PAID trước khi đóng gói/giao; IPN thanh toán giữ nguyên bước xử lý để seller chủ động thao tác. Đơn chưa thanh toán có thể hủy trước khi bàn giao, nếu không có phiên thanh toán đang hiệu lực; tồn kho chỉ hoàn một lần. Khi báo đơn đã thay đổi, chọn **Tải lại dữ liệu** trước khi thao tác tiếp.

Giao hàng hiện là ghi nhận nội bộ thủ công, chưa tạo vận đơn hoặc kết nối hãng vận chuyển. Đánh dấu đã giao không tự xác nhận thu tiền COD. Đơn đã thanh toán không được hủy/hoàn tiền ở chức năng này. Nếu tiền về muộn sau khi hủy, hệ thống ghi nhận PAID và cảnh báo cần đối soát/hoàn tiền riêng, không mở lại đơn. Chi tiết: [seller-order-fulfillment.md](docs/sprints/seller-order-fulfillment.md).

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

## Hướng Dẫn Khởi Chạy (một cụm Docker chính)

Bật Docker Desktop, rồi tại thư mục gốc:

```powershell
.\scripts\Start-Local.ps1
```

Script build và bật **một project `webbnhngonline`**: frontend, backend, MySQL, Redis, RabbitMQ. Tái sử dụng đúng các volume `webbnhngonline_*` cũ và thư mục `backend/uploads`; không chạy seed, không xóa dữ liệu. Script dừng các container `helishop-integration` nếu đang chạy và giữ nguyên volume kiểm thử. Nhiều service nằm trong một project Docker là bình thường.

- Frontend Docker: http://localhost
- Backend/Swagger: http://localhost:8080/swagger-ui/index.html
- Frontend khi sửa code: trong `frontend`, chạy `npm ci` (lần đầu), rồi `npm run dev` → http://localhost:5173. Vite gọi cùng backend 8080; bỏ override cũ `DEV_API_PROXY_TARGET=http://127.0.0.1:18080` trong `.env.development.local` hoặc đổi về `http://127.0.0.1:8080`.

### Email dự phòng và quản lý tài khoản

Mở `.env.gmail.local` do script tạo và điền:

```dotenv
SMTP_USERNAME=your-sender@gmail.com
SMTP_PASSWORD=your-google-app-password
```

Giữ SMTP host `smtp.gmail.com`, cổng `587`, auth/STARTTLS `true`. Bật xác minh hai bước và tạo [Google App Password](https://support.google.com/accounts/answer/185833); điền mã App Password không có khoảng trắng, không dùng mật khẩu đăng nhập Google. Tệp này và `.env` chứa JWT secret không được commit. Sau khi điền/chỉnh mail, chạy:

```powershell
.\scripts\Start-Local.ps1 -NoBuild
```

Backend nạp cấu hình mới. Google Sign-In không cần cấu hình SMTP. SMTP dùng cho hóa đơn và cách đăng ký Gmail + mật khẩu dự phòng; chưa có SMTP thì luồng đăng ký dự phòng này không hoàn tất được. Không có Mailpit trong cụm chính, không có đường bỏ qua OTP. [Thông số SMTP chính thức](https://developers.google.com/workspace/gmail/imap/imap-smtp).

Đăng ký bằng mật khẩu dự phòng: mở phần **Đăng ký bằng Gmail và mật khẩu**, chọn người mua hoặc người bán, nhập `@gmail.com`, chọn **Gửi OTP về Gmail**, nhập 6 số và mật khẩu/xác nhận, rồi đăng ký. Mã có hiệu lực 10 phút, dùng một lần; chờ 60 giây gửi lại. Email cũ tiếp tục đăng nhập được, không dùng số điện thoại. Số điện thoại được nhập trong **Sổ địa chỉ/địa chỉ nhận hàng**, để shipper liên lạc.

Admin đăng nhập được chuyển tới `/admin/users`, hoặc mở menu tài khoản → **Quản lý tài khoản** để tìm/lọc, khóa/mở khóa buyer/seller. Database cũ trên máy có admin; không tạo thêm quyền admin qua đăng ký công khai. Khóa thu hồi phiên hiện tại và giữ dữ liệu đơn/shop; mở khóa phải đăng nhập lại.

### Những lần chạy sau

```powershell
docker compose up -d --wait          # cấu hình không đổi, không cần build lại
docker compose up -d --build --wait  # sau khi sửa code
# hoặc .\scripts\Start-Local.ps1 (tự dừng cụm integration)
docker compose ps
docker compose logs --tail 80 backend
docker compose stop                 # dừng, giữ container và toàn bộ dữ liệu
```

Các service có `restart: unless-stopped`: bật Docker Desktop có thể tự khởi động lại nếu trước đó chưa Stop thủ công. Nếu đã Stop, chạy lại lệnh khởi động. Không chạy thêm `mvnw spring-boot:run` khi backend Docker đang chiếm 8080.

Cấu hình gốc là môi trường local với thông tin kết nối volume cũ và cổng chỉ bind loopback. VNPAY chưa được cấu hình trong cụm local, dùng COD. Để vận hành máy chủ riêng với credentials/ngân hàng/hostname riêng, dùng `Initialize-Environment.ps1` + `docker-compose.production.yml`; đây là lựa chọn triển khai riêng, không phải cụm thứ hai cần bật hằng ngày. Không nạp lại `seed-data.sql` vào database hiện có; script seed không phải migration bảo toàn dữ liệu.

### Kiểm thử tích hợp

CI tự tạo môi trường mới `helishop-integration` với MySQL/Redis/RabbitMQ/Mailpit riêng; không dùng database chính. Local chỉ bật khi chủ động chạy E2E, dừng lại sau kiểm thử. Mailpit giữ email thử, không gửi tới Gmail thật, dù dữ liệu ca thử dùng địa chỉ có đuôi `@gmail.com`. Không chạy test E2E trỏ vào dữ liệu chính. Chi tiết [frontend/README.md](frontend/README.md).

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
