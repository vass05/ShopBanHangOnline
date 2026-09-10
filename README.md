# HeliShop Core E-Commerce Backend (Spring Boot 3.3.5 & Java 21)

Dự án Hệ thống Web Bán Hàng Online - Kiến trúc Domain-Driven / Layered chuẩn doanh nghiệp (`com.helishop.core`).

---

## 🏛️ Kiến trúc Package Chuẩn Doanh nghiệp

Mã nguồn được phân chia thành các Domain Modules độc lập, kết hợp tầng tiện ích dùng chung (Common), tầng Bảo mật (Security) và Cấu hình hệ thống (Config):

```plaintext
src/main/java/com/helishop/core/
├── HeliShopApplication.java    # Lớp Bootstrap Spring Boot
├── common/                     # Tiện ích dùng chung toàn hệ thống
│   ├── base/                   # BaseEntity (id, createdAt, updatedAt, auditing)
│   ├── constants/              # Enums (Role, UserRole, OrderStatus, PaymentStatus, v.v.)
│   ├── exception/              # GlobalExceptionHandler, AppException, ErrorCode, ResourceNotFoundException
│   └── response/               # ApiResponse<T>, PageResponse<T>
├── config/                     # Cấu hình hệ thống (SecurityConfig, RedisConfig, OpenApiConfig, JpaAuditingConfig)
├── modules/
│   ├── auth/                   # Authentication & Token service
│   │   ├── controller/         # AuthController (/api/v1/auth/register, login)
│   │   ├── dto/                # LoginRequest, RegisterRequest, AuthResponse
│   │   └── service/            # AuthService (BCrypt, JWT issue)
│   ├── user/                   # Quản lý tài khoản, profile & shop bán hàng
│   │   ├── entity/             # User, UserAddress, Shop
│   │   ├── repository/         # UserRepository, ShopRepository
│   │   └── service/            # UserService
│   ├── product/                # Hàng hóa, danh mục, kho SKU
│   │   ├── controller/         # ProductController (/api/v1/products)
│   │   ├── dto/                # ProductRequest, ProductResponse, ProductFilterCriteria
│   │   ├── entity/             # Product, Category, ProductSku, ProductImage
│   │   ├── mapper/             # ProductMapper (MapStruct)
│   │   ├── repository/         # ProductRepository, CategoryRepository, ProductSkuRepository, ProductSpecification
│   │   └── service/            # ProductService (Dynamic filter, pagination)
│   └── order/                  # Đơn hàng, thanh toán, locking tồn kho
│       ├── controller/         # OrderController (/api/v1/orders)
│       ├── dto/                # CheckoutRequest, OrderResponse
│       ├── entity/             # Order, OrderItem, CartItem, Voucher, PaymentTransaction
│       ├── repository/         # OrderRepository
│       └── service/            # OrderService (Optimistic Locking)
└── security/                   # JwtUtils, JwtFilter, CustomEntryPoint
```

---

## 🚀 Hạ tầng và Cấu hình

### 1. Hạ tầng Docker Local (`docker-compose.yml`)
- **MySQL 8.0**: Cổng host `3307` (map vào `3306` của container), múi giờ Việt Nam (`+07:00`), volume `mysql_data`.
- **Redis 7-alpine**: Cổng `6379`, volume `redis_data`.

Khởi động hạ tầng:
```bash
docker compose up -d
```

### 2. Bảo mật và OpenAPI Documentation
- **Spring Security & Stateless JWT**: Xác thực qua Bearer Token, kiểm soát quyền truy cập chi tiết (`@PreAuthorize`).
- **Swagger / OpenAPI 3.0**: Truy cập tài liệu API tự động tại `http://localhost:8080/swagger-ui/index.html`.

### 3. Tầng DTO, MapStruct & Xử lý lỗi tập trung (Sprint 2)
- **ApiResponse<T> thống nhất**: Format JSON chuẩn (`code`, `message`, `data`, `errors`, `timestamp`).
- **GlobalExceptionHandler**:
  - Bắt `@Valid` (`MethodArgumentNotValidException`) -> Trích xuất map lỗi từng field trả về HTTP 400.
  - Bắt `ResourceNotFoundException` -> Trả về HTTP 404.
  - Bắt `InsufficientStockException` -> Trả về HTTP 409 (Conflict) khi sản phẩm hết hàng hoặc không đủ số lượng tồn kho.
  - Bắt `BadRequestException` -> Trả về HTTP 400.
- **MapStruct 1.5.x**: Tự động sinh mã nguồn mapper với `componentModel = "spring"`:
  - `ProductMapper`, `CategoryMapper`, `ProductSkuMapper`, `UserMapper`.

### 4. Sprint 3: Security & Token Architecture
- **Stateless RBAC**: Phân quyền chi tiết `ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_ADMIN`.
- **JWT & Redis Refresh Token**: Access Token (HMAC-SHA256, 15 phút) và Refresh Token (7 ngày) lưu trữ trong Redis.
- **SecurityFilterChain**: Public xem sản phẩm; Seller tạo/cập nhật hàng; Customer checkout đơn hàng.

### 5. Sprint 4: Catalog & Dynamic Query Engine
- **JPA Specification**: Lọc đa tiêu chí động theo khoảng giá (`price`), từ khóa (`name`), danh mục con cháu, trạng thái, shop và số sao đánh giá (`rating`).
- **Cây danh mục đệ quy**: Tự động tìm kiếm bao gồm tất cả các nhánh danh mục con cháu.
- **Trị dứt điểm N+1 Query**: Áp dụng `@EntityGraph(attributePaths = {"category", "shop", "skus", "images"})` tải dữ liệu chỉ với 1 câu SQL JOIN.

### 6. Sprint 5: Order Engine, Concurrency Locking & Cache
- **Pessimistic Locking (`PESSIMISTIC_WRITE`)**:
  - `ProductRepository.findByIdWithLock(id)` và `ProductSkuRepository.findByIdWithLock(id)` ngăn chặn triệt để race condition và overselling trong Flash Sale.
  - Luồng `checkoutOrder()` nguyên tử (@Transactional): Khóa tồn kho $\rightarrow$ Kiểm tra tồn kho $\rightarrow$ Trừ tồn kho $\rightarrow$ Lưu Order và OrderItem (chụp snapshot tên, giá, biến thể lúc mua).
- **Quản lý trạng thái & Hoàn tồn kho (@Transactional)**:
  - Khi đơn hàng chuyển sang `CANCELLED` hoặc `RETURNED`, hệ thống tự động hoàn kho lại cho SKU và Product.
- **Redis Cache**:
  - `@Cacheable(value = "products", key = "#id")`: Tối ưu hóa API xem chi tiết sản phẩm, giảm tải truy vấn DB.
  - `@CacheEvict(value = "products", key = "#id")`: Tự động xóa cache khi Seller cập nhật sản phẩm.
  - `@Cacheable(value = "categories", key = "'tree'")`: Cache cấu trúc cây danh mục đa tầng.

### 7. Sprint 6: Dockerize, Swagger OpenAPI & Comprehensive Testing
- **Multi-Stage Dockerfile**:
  - Stage 1: Build JAR với `maven:3.9.9-eclipse-temurin-21-alpine`, tối ưu Docker layer caching dependencies.
  - Stage 2: Runtime image với `eclipse-temurin:21-jre-alpine` (~150MB), non-root user `appuser` (UID 1001), tối ưu cgroup JVM memory flag `-XX:MaxRAMPercentage=75.0`.
  - Tích hợp full stack trong `docker-compose.yml` (`backend` + `mysql` + `redis` trên `eshop_network`).
- **Tài liệu hóa 100% Swagger UI (OpenAPI 3.0)**:
  - Gắn `@Tag`, `@Operation`, `@ApiResponses`/`@ApiResponse` chi tiết mã phản hồi (200, 201, 400, 401, 403, 404, 409) trên toàn bộ 4 Controller: `AuthController`, `ProductController`, `CategoryController`, `OrderController`.
  - Gắn `@Schema(description, example)` trên 100% các DTOs (Request, Response, Criteria, Base ApiResponse, PageResponse).
  - Truy cập tài liệu tương tác tại: `http://localhost:8080/swagger-ui/index.html`.
- **Kiểm thử chuyên sâu (Unit Test & Integration Test)**:
  - `OrderServiceStockUnitTest`: Kiểm tra trừ kho khi mua hợp lệ, kiểm tra ném ngoại lệ `InsufficientStockException` (HTTP 409) khi mua vượt kho, `ResourceNotFoundException`, `AppException`, và kiểm tra logic tự động hoàn kho khi hủy đơn / hoàn hàng.
  - `CheckoutFlowIntegrationTest`: Kiểm thử luồng checkout end-to-end qua MockMvc xác thực phân quyền Spring Security (`ROLE_CUSTOMER` 201 Created, `ROLE_SELLER` 403 Forbidden, Unauthenticated 401 Unauthorized, Hết kho 409 Conflict, Sai payload 400 Bad Request).

---

## 🧪 Kiểm thử và Chạy ứng dụng

### 1. Kiểm thử Validation & Custom Exceptions (Sprint 2)
```powershell
.\mvnw.cmd test -Dtest=ValidationAndExceptionIntegrationTest
```

### 2. Kiểm thử MapStruct Mappers (Sprint 2)
```powershell
.\mvnw.cmd test -Dtest=MapperUnitTest
```

### 3. Kiểm thử Khóa bi quan chống âm kho (Sprint 5)
```powershell
.\mvnw.cmd test -Dtest=PessimisticLockingIntegrationTest
```

### 4. Kiểm thử Cập nhật trạng thái đơn hàng & Hoàn tồn kho (Sprint 5)
```powershell
.\mvnw.cmd test -Dtest=OrderStatusAndReturnIntegrationTest
```

### 5. Kiểm thử Redis Cache @Cacheable & @CacheEvict (Sprint 5)
```powershell
.\mvnw.cmd test -Dtest=RedisCachingTest
```

### 6. Kiểm thử Unit Test trừ kho & ngoại lệ tồn kho (Sprint 6)
```powershell
.\mvnw.cmd test -Dtest=OrderServiceStockUnitTest
```

### 7. Kiểm thử Integration Test API Checkout (Sprint 6)
```powershell
.\mvnw.cmd test -Dtest=CheckoutFlowIntegrationTest
```

### 8. Chạy toàn bộ Test Suite (57 tests)
```powershell
.\mvnw.cmd test
```

### 9. Khởi chạy Server Local
```powershell
.\mvnw.cmd spring-boot:run
```

### 10. Đóng gói & Khởi chạy Full Stack với Docker Compose (Sprint 6)
```bash
docker compose up --build -d
```
Truy cập Swagger UI sau khi container chạy:
`http://localhost:8080/swagger-ui/index.html`


