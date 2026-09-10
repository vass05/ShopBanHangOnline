# BỘ NHỚ TRẠNG THÁI HỆ THỐNG: HELISHOP CORE (LIVING MEMORY)

Tài liệu này lưu trữ trạng thái ngữ cảnh thực tế của dự án, các thông số kết nối, tài khoản kiểm thử, kiến trúc hiện thời và các bài học kỹ thuật quan trọng nhằm loại bỏ rủi ro quên ngữ cảnh giữa các phiên làm việc.

---

## 📌 1. TRẠNG THÁI HIỆN THỜI (CURRENT STATE)

- **Giai đoạn đang thi công**: **Project 2: Shopee E-Commerce Integration & Automation**
- **Sprint hiện tại**: Hoàn thành **Sprint 3 (Async Email Worker, RabbitMQ Retry/DLQ, Media Upload Cloudinary 1:1)**. Chuẩn bị bước vào **Sprint 4 (Hủy đơn hàng, Hoàn trả kho nguyên tử & Dead Letter Alerting Engine)**.
- **Nhánh Git**: `main` (đồng bộ hoàn toàn với `https://github.com/vass05/ShopBanHangOnline.git`).
- **Tổng số Unit & Integration Tests**: **98 bài kiểm thử - 100% PASSED**.

---

## 💻 2. CÔNG NGHỆ VÀ PHIÊN BẢN (TECH STACK MATRIX)

| Thành phần | Công nghệ / Thư viện | Phiên bản | Ghi chú cấu hình |
| :--- | :--- | :---: | :--- |
| **Ngôn ngữ** | Java | 21 (Temurin / OpenJDK) | Cú pháp Pattern Matching, Record, Virtual Threads ready |
| **Framework** | Spring Boot | 3.3.5 | Spring Security 6, Spring Data JPA, Spring AMQP |
| **Cơ sở dữ liệu** | MySQL | 8.0 | Port 3307 (Host) -> 3306 (Container), utf8mb4 |
| **Bộ nhớ đệm / Giỏ hàng** | Redis | 7-alpine | Port 6379, AOF Persistent (`appendonly yes`) |
| **Message Broker** | RabbitMQ | 3.13-management | Port 5672 (AMQP), 15672 (Management UI) |
| **Tài liệu API** | Springdoc OpenAPI (Swagger 3) | 2.6.0 | UI: `http://localhost:8080/swagger-ui/index.html` |
| **Xác thực** | JJWT (io.jsonwebtoken) | 0.12.6 | Access Token (15 phút), Refresh Token (7 ngày) |
| **Object Mapper** | MapStruct | 1.5.5.Final | Lombok binding 0.2.0 |
| **Lombok** | Project Lombok | 1.18.34 | Cần gán cứng `<lombok.version>1.18.34</lombok.version>` |

---

## 🌐 3. BẢNG THÔNG SỐ CỔNG & TÀI KHOẢN MẶC ĐỊNH

### 3.1. Cổng Dịch Vụ
- **Application Backend**: `http://localhost:8080`
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

---

## 📋 5. SẴN SÀNG CHO SPRINT TIẾP THEO: SPRINT 4
- **Mục tiêu chính**:
  1. Hủy đơn hàng và cơ chế hoàn trả số lượng tồn kho nguyên tử (Atomic Stock Compensation).
  2. Dead Letter Alerting Engine: Xử lý và cảnh báo các message rơi vào `order.email.dlq` hoặc queue DLQ khác.
  3. Quản lý trạng thái đơn hàng (PENDING -> PROCESSING -> SHIPPED -> DELIVERED / CANCELLED).

