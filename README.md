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

---

## 🧪 Kiểm thử và Chạy ứng dụng

### 1. Chạy bài test Validation & Custom Exceptions (Sprint 2)
```powershell
.\mvnw.cmd test -Dtest=ValidationAndExceptionIntegrationTest
```

### 2. Chạy bài test MapStruct Mappers (Sprint 2)
```powershell
.\mvnw.cmd test -Dtest=MapperUnitTest
```

### 3. Chạy toàn bộ Test Suite
```powershell
.\mvnw.cmd test
```

### 4. Khởi chạy Server
```powershell
.\mvnw.cmd spring-boot:run
```
