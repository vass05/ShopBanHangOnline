# E-Commerce Backend (Spring Boot 3.3.5 & Java 21)

Dự án Hệ thống Web Bán Hàng Online - Kiến trúc Micro-Core sạch, tuân thủ chuẩn Domain-Driven Design và JPA tối ưu hiệu năng cao.

---

## 🎯 Kết quả hoàn thành Sprint 1: Setup Infrastructure & Core Entity Modeling

### 1. Hạ tầng Docker Local (`docker-compose.yml`)
- **MySQL 8.0**: Cổng host `3307` (map vào `3306` của container), thiết lập múi giờ Việt Nam (`+07:00`), volume `mysql_data` lưu trữ bền vững.
- **Redis 7-alpine**: Cổng `6379`, volume `redis_data`.

Khởi động hạ tầng:
```bash
docker compose up -d
```

### Kết nối qua MySQL Workbench:
- **Hostname**: `127.0.0.1` (hoặc `localhost`)
- **Port**: `3307`
- **Username**: `eshop_user` *(hoặc `root`)*
- **Password**: `eshop_secret` *(hoặc `root_secret`)*
- **Default Schema**: `eshop_db`
Kiểm tra trạng thái container:
```bash
docker compose ps
```

---

### 2. Tầng Base Entity & Kích hoạt JPA Auditing
- `BaseEntity.java`: Kế thừa `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`.
  - Khóa chính `id`: `BIGINT AUTO_INCREMENT` (`GenerationType.IDENTITY`).
  - `createdAt`: Tự động điền timestamp khi tạo (`@CreatedDate`).
  - `updatedAt`: Tự động cập nhật timestamp khi sửa đổi (`@LastModifiedDate`).
- `JpaAuditingConfig.java`: Kích hoạt `@EnableJpaAuditing`.
- Hệ thống Enums dùng chung trong `com.eshop.common.enums`:
  - `UserRole`: `ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_ADMIN`
  - `UserStatus`: `ACTIVE`, `INACTIVE`, `BLOCKED`
  - `ProductStatus`: `DRAFT`, `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`
  - `OrderStatus`: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`, `DELIVERED`, `CANCELLED`, `RETURNED`
  - `PaymentStatus`: `UNPAID`, `PENDING`, `PAID`, `FAILED`, `REFUNDED`
  - `PaymentMethod`: `COD`, `VNPAY`, `MOMO`, `BANK_TRANSFER`
  - `DiscountType`: `FIXED_AMOUNT`, `PERCENTAGE`
  - `ShopStatus`: `ACTIVE`, `INACTIVE`, `BANNED`
  - `VoucherStatus`: `ACTIVE`, `EXPIRED`, `DISABLED`

---

### 3. Ánh xạ 12 Core JPA Entities (100% `FetchType.LAZY`)
Toàn bộ 12 Entity đều kế thừa từ `BaseEntity` và mọi quan hệ `@ManyToOne`, `@OneToOne`, `@OneToMany` đều được thiết lập `fetch = FetchType.LAZY` tuyệt đối để ngăn ngừa lỗi N+1 Query:

1. **`User`**: Tài khoản người dùng, email (unique), passwordHash, phone, role, status.
2. **`UserAddress`**: Sổ địa chỉ giao hàng của người dùng, liên kết `@ManyToOne(fetch = LAZY) User`.
3. **`Shop`**: Gian hàng người bán, liên kết `@OneToOne(fetch = LAZY) User owner`.
4. **`Category`**: Cây danh mục tự tham chiếu `@ManyToOne(fetch = LAZY) Category parent` và danh sách con `@OneToMany(fetch = LAZY)`.
5. **`Product`**: Sản phẩm, liên kết `@ManyToOne(fetch = LAZY) Shop` và `@ManyToOne(fetch = LAZY) Category`.
6. **`ProductSku`**: Đơn vị lưu kho SKU:
   - `skuCode`, `price`, `originalPrice`, `stockQuantity`.
   - `@Version private Long version;` hỗ trợ **Optimistic Locking** ngăn ngừa over-selling (bán vượt tồn kho) khi có nhiều request đồng thời.
7. **`ProductImage`**: Thư viện ảnh sản phẩm, liên kết `@ManyToOne(fetch = LAZY) Product`.
8. **`CartItem`**: Giỏ hàng liên kết `@ManyToOne(fetch = LAZY) User` và `@ManyToOne(fetch = LAZY) ProductSku`.
9. **`Voucher`**: Mã giảm giá, liên kết `@ManyToOne(fetch = LAZY) Shop` (nullable cho voucher toàn sàn).
10. **`Order`**: Đơn đặt hàng, snapshot địa chỉ `shippingAddressSnapshot`, liên kết `@ManyToOne(fetch = LAZY) User customer` và `Shop`.
11. **`OrderItem`**: Chi tiết đơn hàng chứa các snapshot giá trị thời điểm mua: `priceAtPurchase`, `productNameSnapshot`, `skuVariantSnapshot`.
12. **`PaymentTransaction`**: Giao dịch thanh toán liên kết `@ManyToOne(fetch = LAZY) Order`.

---

### 4. Đánh chỉ mục Database (Performance Indexing)
- **`Product`**: `category_id`, `shop_id`, `slug`
- **`ProductSku`**: `product_id`, `price`, `sku_code`
- **`Order`**: Composite index `(customer_id, created_at)`, `order_code`, `shop_id`
- **`CartItem`**: `user_id`, `sku_id`
- **`PaymentTransaction`**: `order_id`, `transaction_code`
- **`UserAddress`**: `user_id`
- **`Category`**: `parent_id`, `slug`
- **`Shop`**: `owner_id`, `shop_name`
- **`Voucher`**: `voucher_code`, `shop_id`

---

### 5. Hướng dẫn chạy và kiểm thử

#### Chạy kiểm thử tự động (Unit & Integration Tests)
Sử dụng Maven wrapper được chuẩn bị sẵn:
```powershell
.\mvnw.cmd test
```
Bài kiểm thử `UserPersistenceTest` sẽ:
1. Xác minh việc lưu `User`, tự động sinh khóa chính BIGINT IDENTITY và tự động điền `createdAt`/`updatedAt` nhờ JPA Auditing.
2. Xác minh việc lưu `ProductSku` với `@Version` được khởi tạo bằng `0` phục vụ khóa lạc quan.

#### Chạy ứng dụng Spring Boot
```powershell
.\mvnw.cmd spring-boot:run
```
Hibernate sẽ tự động đọc `application.yml` và đồng bộ schema vào database MySQL với log SQL được định dạng chi tiết.
