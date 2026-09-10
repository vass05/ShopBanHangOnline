# BỘ NHỚ TRẠNG THÁI HỆ THỐNG: HELISHOP CORE (LIVING MEMORY)

Tài liệu này lưu trữ trạng thái ngữ cảnh thực tế của dự án, các thông số kết nối, tài khoản kiểm thử, kiến trúc hiện thời và các bài học kỹ thuật quan trọng nhằm loại bỏ rủi ro quên ngữ cảnh giữa các phiên làm việc.

---

## 📌 1. TRẠNG THÁI HIỆN THỜI (CURRENT STATE)

- **Giai đoạn đang thi công**: **Project 2: Shopee E-Commerce Integration & Automation (HOÀN THÀNH 100% 6/6 SPRINTS)**
- **Sprint hiện tại**: Hoàn thành **Sprint 6 (End-to-End Testing, CI/CD & Đóng gói Docker Production)**.
- **Nhánh Git**: `main` (đồng bộ hoàn toàn với `https://github.com/vass05/ShopBanHangOnline.git`).
- **Backend Tests**: **103 bài kiểm thử - 100% PASSED** (Bao gồm E2E VNPAY, Idempotent Webhook, SMTP Fault Tolerance DLQ, và Concurrency Stress Test 200 Threads).
- **Frontend Build**: **Vite Production Bundle Built Successfully (0 TypeScript errors, 1704 modules)**.
- **Production Infrastructure**: `docker-compose.production.yml` tích hợp 5 dịch vụ khép kín (`frontend` Nginx reverse proxy, `backend` Spring Boot, `mysql` 8.0, `redis` 7 AOF, `rabbitmq` 3.13 DLQ).
- **Tone màu thương hiệu**: Ocean Blue (`#0284C7`, `#0369A1`, `#0EA5E9`), nền xám mềm (`#F5F5FA`), phối viền sạch sẽ. Mọi thành phần đều tuân thủ màu sắc đặc trưng của HeliShop.

---

## 💻 2. CÔNG NGHỆ VÀ PHIÊN BẢN (TECH STACK MATRIX)

| Thành phần | Công nghệ / Thư viện | Phiên bản | Ghi chú cấu hình |
| :--- | :--- | :---: | :--- |
| **Backend Java** | OpenJDK / Temurin | 21 | Pattern Matching, Record, Virtual Threads ready |
| **Backend Framework** | Spring Boot | 3.3.5 | Spring Security 6, Spring Data JPA, Spring AMQP |
| **Frontend UI** | React | 18.3.1 | Vite 5.4.9, TypeScript 5.6.3 |
| **Styling** | Tailwind CSS | 3.4.14 | Shopee Orange Theme `#EE4D2D`, Glassmorphism, Inter Font |
| **State Management** | Zustand | 5.0.0 | Persist middleware, LocalStorage sync |
| **Server State / Cache** | TanStack Query | 5.59.16 | StaleTime 5m, GC 15m |
| **HTTP Client** | Axios | 1.7.7 | Request/Response Interceptor với hàng đợi `failedQueue` |
| **Icons** | Lucide React | 0.453.0 | Icon pack hiện đại |
| **Cơ sở dữ liệu** | MySQL | 8.0 | Port 3307 (Host) -> 3306 (Container), utf8mb4 |
| **Bộ nhớ đệm / Giỏ hàng** | Redis | 7-alpine | Port 6379, AOF Persistent (`appendonly yes`) |
| **Message Broker** | RabbitMQ | 3.13-management | Port 5672 (AMQP), 15672 (Management UI) |
| **Tài liệu API** | Springdoc OpenAPI (Swagger 3) | 2.6.0 | UI: `http://localhost:8080/swagger-ui/index.html` |
| **Xác thực** | JJWT (io.jsonwebtoken) | 0.12.6 | Access Token (15 phút), Refresh Token (7 ngày) |

---

## 🌐 3. BẢNG THÔNG SỐ CỔNG & TÀI KHOẢN MẶC ĐỊNH

### 3.1. Cổng Dịch Vụ
- **Application Backend**: `http://localhost:8080`
- **Frontend Web Application**: `http://localhost:5173` (React 18 + Vite)
- **Swagger Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **MySQL Database**: `localhost:3307` (user: `eshop_user`, pass: `eshop_secret`, db: `eshop_db`)
- **Redis Server**: `localhost:6379`
- **RabbitMQ Dashboard**: `http://localhost:15672` (user: `guest`, pass: `guest`)
- **RabbitMQ AMQP**: `localhost:5672`

### 3.2. Cặp Khóa Bí Mật & TTL
- **JWT Secret**: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
- **Access Token TTL**: 15 phút (`900,000` ms)
- **Refresh Token TTL**: 7 ngày (`604,800` giây)
- **Redis Cart Hash TTL**: 30 ngày (`2,592,000` giây)

---

## ⚠️ 4. BẪY KỸ THUẬT & GIẢI PHÁP ĐÃ ĐƯỢC XÁC LẬP (PITFALLS & WORKAROUNDS)

### 1. Lỗi xung đột tên ApiResponse trong Controller
- **Vấn đề**: `io.swagger.v3.oas.annotations.responses.ApiResponse` bị trùng tên với `com.helishop.core.common.response.ApiResponse`.
- **Giải pháp bắt buộc**: Không import Swagger ApiResponse. Luôn chú thích dạng đầy đủ: `@io.swagger.v3.oas.annotations.responses.ApiResponse(...)`.

### 2. Xung đột Annotation Processor (Lombok 1.18.34 & MapStruct)
- **Vấn đề**: Khi compile Maven trên Java 21, Lombok mặc định của Spring Boot parent có thể gây lỗi nạp MapStruct.
- **Giải pháp**: Định nghĩa rõ ràng thuộc tính `<lombok.version>1.18.34</lombok.version>` trong `pom.xml`.

### 3. Lỗi PowerShell Command Chaining
- **Vấn đề**: PowerShell không chấp nhận toán tử `&&` để nối lệnh trong các phiên bản cũ hoặc cấu hình mặc định (VD: `git add . && git commit`).
- **Giải pháp**: Luôn dùng dấu chấm phẩy `;` để phân tách lệnh: `git add . ; git commit -m "..."`.

### 4. Lỗi PowerShell đối số có dấu phẩy
- **Vấn đề**: Lệnh như `mvn test -Dtest=TestA,TestB` bị PowerShell hiểu nhầm dấu phẩy là phần tử mảng.
- **Giải pháp**: Luôn bọc đối số trong ngoặc kép: `mvn test "-Dtest=TestA,TestB"`.

### 5. Kiểm thử Mockito Strictness với RedisTemplate
- **Vấn đề**: Mockito 5 mặc định strict stubbing. Nếu stub `stringRedisTemplate.opsForHash()` trong `@BeforeEach` nhưng test case gọi `clearCart()` (chỉ gọi `delete()`), Mockito sẽ ném lỗi `UnnecessaryStubbingException`.
- **Giải pháp**: Sử dụng `lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);`.

### 6. Định dạng phản hồi VNPAY IPN Webhook Server-to-Server
- **Vấn đề**: VNPAY IPN yêu cầu định dạng JSON chính xác `{"RspCode":"00","Message":"Confirm Success"}` với trường viết hoa `RspCode` và `Message`. Nếu bọc trong `ApiResponse<T>` chuẩn của hệ thống, máy chủ VNPAY sẽ báo lỗi không nhận dạng được và liên tục gọi lại.
- **Giải pháp**: Tạo DTO `VnPayIpnResponse` với `@JsonProperty("RspCode")` và `@JsonProperty("Message")`, trả về trực tiếp từ controller `/api/v1/payments/vnpay-ipn`.

### 7. Spring Boot MailSenderAutoConfiguration trong môi trường Test
- **Vấn đề**: `MailSenderAutoConfiguration` chỉ tạo bean `JavaMailSender` khi thuộc tính `spring.mail.host` tồn tại. Nếu `src/test/resources/application.yml` thiếu `spring.mail.host`, toàn bộ `@SpringBootTest` quét qua `EmailService` sẽ gặp lỗi `UnsatisfiedDependencyException`.
- **Giải pháp**: Định nghĩa `spring.mail.host: localhost` trong `src/test/resources/application.yml`.

### 8. Biến @Value trong Mockito Unit Test thuần túy
- **Vấn đề**: Các test case chạy bằng `@ExtendWith(MockitoExtension.class)` không khởi tạo Spring Context, dẫn đến các trường có `@Value` bị null nếu không được gán qua reflection.
- **Giải pháp**: Luôn gán giá trị mặc định trực tiếp lúc khai báo trường trong class (ví dụ: `private String senderEmail = "helishop.system@gmail.com";`).

### 9. Cấu hình CORS Backend & Vite Proxy
- **Vấn đề**: Khi chạy frontend Vite trên `http://localhost:5173`, nếu Spring Security chặn CORS với `AbstractHttpConfigurer::disable` thì trình duyệt sẽ chặn toàn bộ các request preflight `OPTIONS`.
- **Giải pháp**: Cấu hình `CorsConfigurationSource` cho phép `allowedOriginPatterns("*")`, methods, headers `*` và `allowCredentials(true)` trong `SecurityConfig.java`, đồng thời thiết lập Vite proxy forwarding `/api` trực tiếp về `http://localhost:8080`.

### 10. Chống Race Condition Refresh Token với failedQueue
- **Vấn đề**: Khi Access Token hết hạn, nếu trang web cùng lúc bắn ra 5-10 request song song (ví dụ: lấy thông tin user, giỏ hàng, thông báo, danh mục), tất cả các request này đều nhận mã 401. Nếu không có cơ chế hàng đợi, cả 10 request sẽ đồng thời gọi `POST /auth/refresh-token`, dẫn tới việc Refresh Token bị thu hồi hoặc vi phạm tính toàn vẹn phiên làm việc.
### 11. Cơ chế Nạp dữ liệu mẫu bằng SQL (Manual Seed Script) & Quy chuẩn Tài khoản
- **Đặc điểm**: Hệ thống chuyển hẳn sang sử dụng tệp SQL chi tiết [backend/src/main/resources/seed-data.sql](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/backend/src/main/resources/seed-data.sql) chuẩn MySQL 8.0, loại bỏ hoàn toàn `DatabaseSeeder.java` để người dùng toàn quyền kiểm soát thời điểm và dữ liệu nạp vào DB.
- **Ràng buộc đăng ký**:
  1. Mỗi Gmail chỉ được đăng ký duy nhất 1 tài khoản (ràng buộc `UNIQUE` trên cột `email` và kiểm tra `userRepository.existsByEmail` tại `AuthService.register`).
  2. Mỗi số điện thoại chỉ liên kết với 1 tài khoản duy nhất (`userRepository.existsByPhone`).
  3. Hỗ trợ đăng nhập linh hoạt bằng cả Gmail hoặc Số điện thoại (`findByEmail(id).or(() -> findByPhone(id))`).

### 12. Tài khoản kiểm thử chuẩn xác hệ thống
- **Khách hàng chính (CUSTOMER)**:
  - **Họ và tên**: `Vũ Viết Anh`
  - **Gmail**: `vuvietanh@gmail.com`
  - **Số điện thoại**: `0988889999`
  - **Mật khẩu**: `Password123!`
  - **Địa chỉ giao hàng mặc định**: `Thôn Tốt Động, Xã Quảng Bị, Huyện Chương Mỹ, TP. Hà Nội`
- **Quản trị viên sàn (ADMIN)**: `admin.helishop@gmail.com` (hoặc `0901111111`) / `Password123!`
- **Người bán hàng Shopee Mall (SELLER)**:
  - Apple Store: `seller.apple@gmail.com` (hoặc `0902222222`) / `Password123!`
  - Sony Official: `seller.sony@gmail.com` (hoặc `0903333333`) / `Password123!`
  - NuPhy Studio: `seller.nuphy@gmail.com` (hoặc `0904444444`) / `Password123!`
  - Coolmate Store: `seller.coolmate@gmail.com` (hoặc `0905555555`) / `Password123!`
  - Logitech G Store: `seller.logitech@gmail.com` (hoặc `0906666666`) / `Password123!`
  - Anker Store: `seller.anker@gmail.com` (hoặc `0907777777`) / `Password123!`
- **Vouchers có sẵn**: `HELI50K` (giảm 50.000₫), `HELI100K` (giảm 100.000₫), `HELI500K` (giảm 500.000₫), `VNPAY10` (giảm 10%), `FREESHIP` (miễn phí vận chuyển 30.000₫).

---

## 📋 5. BÀN GIAO & VẬN HÀNH HỆ THỐNG
- **Loại bỏ hoàn toàn mockData**: Tệp `frontend/src/data/mockData.ts` đã được xóa bỏ hoàn toàn. Frontend hiện kết nối 100% trực tiếp với cơ sở dữ liệu MySQL 8.0 thông qua hệ thống REST APIs của Spring Boot (`/api/v1/products`, `/api/v1/categories/tree`, `/api/v1/orders`).
- **Giao diện khi Backend chưa bật / chưa nạp data**: Hiển thị bảng thông báo trạng thái kết nối máy chủ rõ ràng kèm hướng dẫn khởi động Spring Boot và nạp `seed-data.sql`, tích hợp nút "Thử kết nối lại" (Retry).
- **Trạng thái giỏ hàng & Đơn hàng**: Khởi tạo ban đầu sạch sẽ (rỗng), chỉ hiển thị các sản phẩm và đơn hàng thực tế do người dùng thêm hoặc đặt hàng qua API.
- **Tình trạng kiểm thử**: 100% kiểm thử Backend (103/103 tests) và đóng gói Frontend (`vite build`) đạt chuẩn tuyệt đối. Toàn bộ mã nguồn đã đồng bộ với GitHub `origin/main`.




