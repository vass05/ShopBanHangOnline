# TÀI LIỆU THIẾT KẾ KIẾN TRÚC HỆ THỐNG: PROJECT 2

Tài liệu mô tả chi tiết kiến trúc tổng thể, sơ đồ luồng dữ liệu, cấu trúc dữ liệu giỏ hàng trên Redis và cơ chế xử lý bất đồng bộ qua RabbitMQ.

---

## 1. SƠ ĐỒ KIẾN TRÚC MỞ RỘNG (SYSTEM ARCHITECTURE MATRIX)

```mermaid
flowchart TD
    Client["React 18 + Vite (Shopee UI)"]
    AxiosInterceptor["Axios Interceptor (Silent Refresh Token)"]
    SpringBootCore["Spring Boot 3.3.5 Core Backend"]
    Redis["Redis 7 (AOF Persistent)"]
    MySQL["MySQL 8.0 (Source of Truth)"]
    RabbitMQ["RabbitMQ 3.13 Broker"]
    EmailWorker["Async Email Worker"]
    PaymentGateway["VNPAY IPN Webhook"]

    Client -->|API Requests + Bearer Token| AxiosInterceptor
    AxiosInterceptor --> SpringBootCore
    
    SpringBootCore -->|Hash Ops / Session / Refresh Tokens| Redis
    SpringBootCore -->|Transactions & Pessimistic Locks| MySQL
    
    SpringBootCore -->|Publish Order Paid / Created Event| RabbitMQ
    RabbitMQ -->|Queue: order.email.queue| EmailWorker
    PaymentGateway -->|Verify HMAC-SHA512 & Update Order| SpringBootCore
```

---

## 2. ĐỘNG CƠ GIỎ HÀNG REDIS (REDIS CART ENGINE SPECIFICATION)

### 2.1. Cấu trúc Key và Field
- **Redis Key**: `cart:user:{userId}` (Kiểu dữ liệu: **HASH**)
- **Field (Subkey)**: `{skuId}` (Mã định danh biến thể sản phẩm)
- **TTL**: **30 ngày (2,592,000 giây)**, tự động gia hạn mỗi khi giỏ hàng được cập nhật.

### 2.2. Cấu trúc Payload Value (JSON String)
```json
{
  "skuId": 105,
  "productId": 42,
  "shopId": 3,
  "shopName": "Anker Official Store",
  "productTitle": "Củ sạc nhanh Anker 20W GaN",
  "skuVariant": "Màu Trắng, Chân tròn",
  "imageUrl": "https://res.cloudinary.com/.../anker-white.jpg",
  "price": 250000.00,
  "quantity": 2,
  "stockAvailable": 50,
  "updatedAt": 1773289200
}
```

### 2.3. Cấu trúc Trả về Client (Phân nhóm theo Shop chuẩn Shopee)
```json
{
  "code": 200,
  "message": "Lấy thông tin giỏ hàng thành công",
  "data": {
    "userId": 1,
    "shops": [
      {
        "shopId": 3,
        "shopName": "Anker Official Store",
        "items": [
          {
            "skuId": 105,
            "productId": 42,
            "shopId": 3,
            "shopName": "Anker Official Store",
            "productTitle": "Củ sạc nhanh Anker 20W GaN",
            "skuVariant": "Màu Trắng, Chân tròn",
            "imageUrl": "https://res.cloudinary.com/.../anker-white.jpg",
            "price": 250000.00,
            "quantity": 2,
            "stockAvailable": 50,
            "updatedAt": 1773289200
          }
        ],
        "shopTotal": 500000.00,
        "shopItemCount": 2
      }
    ],
    "totalItems": 1,
    "totalQuantity": 2,
    "totalAmount": 500000.00
  },
  "timestamp": "2026-09-10T15:00:00"
}
```

---

## 3. ĐỊNH TUYẾN RABBITMQ & DEAD LETTER QUEUE (DLQ)

```mermaid
graph LR
    Publisher["Order Service / Payment IPN"] -->|Publish| DirectExchange["DirectExchange: order.direct.exchange"]
    
    DirectExchange -->|Routing: order.paid.email| EmailQueue["Queue: order.email.queue<br/>(Retry x3)"]
    DirectExchange -->|Routing: order.paid.seller| SellerQueue["Queue: order.seller-notify.queue"]
    
    EmailQueue -->|Thất bại sau 3 lần retry| DLX["DLX: order.dlx.exchange"]
    DLX -->|Routing: order.email.dlq| DLQ["Queue: order.email.dlq<br/>(Lưu trữ thông báo lỗi)"]
    
    EmailQueue -->|Thành công| EmailWorker["Email Worker (MimeMessage Async)"]
```

---

## 4. LUỒNG CHECKOUT TỪ REDIS CART VÀO MYSQL (HYBRID TRANSACTION)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    participant Controller as Cart & Order Controller
    participant Redis as Redis Hash Cart
    participant Service as Order Service
    participant DB as MySQL Database
    participant Broker as RabbitMQ Broker

    Customer->>Controller: Bấm "Đặt hàng" (Checkout selected skuIds)
    Controller->>Redis: Đọc dữ liệu các skuId được chọn
    Redis-->>Controller: Trả về danh sách CartItemDto
    Controller->>Service: Gọi checkout(@Transactional)
    loop Khóa bi quan từng SKU
        Service->>DB: SELECT * FROM product_skus WHERE id = ? FOR UPDATE
        DB-->>Service: Khóa SKU bản ghi thành công
        Service->>Service: Kiểm tra tồn kho >= số lượng đặt
        Service->>DB: UPDATE product_skus SET stock_quantity = stock_quantity - ?
    end
    Service->>DB: INSERT INTO orders & order_items
    Service->>Redis: HDEL cart:user:{userId} skuId1 skuId2...
    Service->>Broker: Publish OrderPaidEvent (order.paid.email)
    Service-->>Customer: Trả về OrderResponse (Mã đơn hàng, trạng thái PENDING)
```
