# HeliShop Core E-Commerce - AI Agent Working Guidelines & Memory Protocol

Tệp quy chuẩn hoạt động cốt lõi cho toàn bộ AI Agents, Subagents và lập trình viên làm việc trên repository **HeliShop Core / ShopBanHangOnline**. Mọi tác vụ phát triển, nâng cấp và bàn giao sprint phải tuân thủ nghiêm ngặt các quy tắc dưới đây.

---

## 🚨 1. Quy Tắc Bắt Buộc: Git Commit & Push Policy (QUAN TRỌNG NHẤT)

> [!CAUTION]
> **Tuyệt đối không được kết thúc Sprint hoặc bàn giao tính năng mà chỉ commit ở local!**
> Mọi thay đổi sau khi hoàn tất kiểm thử đều phải được đẩy lên GitHub `origin/main` ngay lập tức.

### Quy trình kết thúc task / sprint chuẩn:
1. **Kiểm thử tự động**: Chạy kiểm thử toàn diện:
   ```powershell
   .\mvnw.cmd test
   ```
   Chỉ khi **100% bài kiểm thử PASSED** mới được tiến hành đóng gói commit.
2. **Commit chuẩn mực (Conventional Commits)**:
   - Cú pháp: `<type>(<scope>): <mô tả ngắn gọn>`
   - Ví dụ: `feat(sprint-1): implement rabbitmq with dlq, redis persistent, and shopee redis cart engine`
3. **Đẩy ngay lên Remote Repository**:
   ```powershell
   git push origin main
   ```
4. **Kiểm tra trạng thái**: Xác nhận `git status` trả về `working tree clean` và commit đã nằm trên remote `origin/main`.

---

## 🏛️ 2. Quy Chuẩn Kiến Trúc & Thiết Kế API

### 2.1. Cấu trúc phản hồi JSON
- Tất cả API Controller **bắt buộc** đóng gói dữ liệu phản hồi trong lớp `ApiResponse<T>`:
  - Trường hợp thành công: `return ApiResponse.success(data, message);` (code = 200).
  - Không bao giờ trả về trực tiếp DTO trần hoặc Map không chuẩn.

### 2.2. Tài liệu hóa OpenAPI / Swagger 3.0
- Mọi Controller phải có annotation `@Tag(name = "...", description = "...")`.
- Mọi Endpoint phải có `@Operation(summary = "...", description = "...")` và `@ApiResponses`.
- **Cảnh báo xung đột**: Luôn dùng `@io.swagger.v3.oas.annotations.responses.ApiResponse(...)` để tránh nhầm lẫn với `com.helishop.core.common.response.ApiResponse`.

### 2.3. Xử lý Ngoại lệ tập trung
- Bắn ngoại lệ qua `AppException(ErrorCode.<MA_LOI>, "Thông điệp chi tiết")`.
- Không bắt nuốt ngoại lệ (`catch (Exception e) {}`) làm ẩn lỗi hệ thống.

---

## ⚡ 3. Quy Chuẩn Công Nghệ & Hiệu Năng

### 3.1. Redis Cart Engine (Project 2)
- Toàn bộ thao tác giỏ hàng thực hiện trên Redis HASH:
  - Key: `cart:user:{userId}`
  - Field: `{skuId}`
  - Value: JSON chuỗi [CartItemDto](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/src/main/java/com/helishop/core/modules/cart/dto/CartItemDto.java)
- TTL giỏ hàng: Luôn duy trì **30 ngày (2,592,000 giây)**. Mỗi lần thêm/sửa số lượng đều phải gia hạn TTL.
- Phân nhóm Shopee: Dữ liệu giỏ hàng trả về phía Client phải luôn được gom nhóm theo từng Shop (`CartShopGroupResponse`).

### 3.2. Quản lý Tồn kho & Flash Sale
- Khi đặt hàng (Checkout), **bắt buộc** dùng Khóa bi quan (`PESSIMISTIC_WRITE`) qua `ProductSkuRepository.findByIdWithLock(skuId)` để chống âm kho trong môi trường đồng thời cao.

### 3.3. RabbitMQ & Asynchronous Worker
- Exchange trao đổi sự kiện đơn hàng: `order.direct.exchange`
- Mọi Queue quan trọng (Email, Thanh toán) phải cấu hình Dead Letter Exchange `order.dlx.exchange` và DLQ `order.email.dlq` để tránh mất dữ liệu khi worker gặp lỗi.

---

## 📝 4. Ghi Nhận Bộ Nhớ & Tiến Độ (Living Memory)

- Sau mỗi Sprint, cập nhật ngay trạng thái vào:
  - [docs/ROADMAP.md](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/docs/ROADMAP.md)
  - [docs/MEMORY.md](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/docs/MEMORY.md)
- Tạo tài liệu tóm tắt sprint tương ứng trong thư mục `docs/sprints/`.
