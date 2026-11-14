# Mở gian hàng — 14/11/2026

Nhánh: `feat/seller-shop-onboarding`, bàn giao qua PR vào `main` theo yêu cầu mỗi chức năng một nhánh. Baseline `22e29d3` (PR #11). Chỉ triển khai mở/xem gian hàng; người dùng sẽ yêu cầu chức năng tiếp theo riêng.

## Hành vi đã triển khai

1. Chọn Kênh Người Bán → `/register?role=seller`, đăng ký SELLER và vào `/seller/shop`. Người bán đã có tài khoản đăng nhập mặc định vào trang này. Redirect nội bộ hợp lệ vẫn được ưu tiên.
2. Nếu chưa có shop, nhập tên 3–150 ký tự và mô tả tùy chọn tối đa 2000 ký tự. Server chuẩn hóa khoảng trắng đầu/cuối, xác định owner và trạng thái ACTIVE.
3. Mở thành công hiển thị tên, mã, trạng thái, mô tả và ngày mở. Tải lại/đăng nhập lại đọc cùng shop từ database. Không dựng số liệu bán hàng hoặc dashboard giả.
4. Trùng tên hoặc tài khoản đã có shop trả 409, lỗi validation 400, chưa đăng nhập 401, sai role hoặc tài khoản không hoạt động 403. Form giữ dữ liệu khi lưu lỗi; tải lỗi yêu cầu thử lại trước khi hiện form. Nếu tab khác vừa tạo shop, khi gặp 409 trang đọc lại shop đã lưu.

## Hợp đồng và bảo đảm

| API | Request | Response |
| --- | --- | --- |
| GET `/api/v1/shops/me` | JWT SELLER, không nhận owner từ query | HTTP 200, ApiResponse chứa ShopResponse; khi chưa có shop thì không có trường data do NON_NULL |
| POST `/api/v1/shops/me` | `{ "shopName": "Tên shop", "description": "Tùy chọn" }` | HTTP 201, ApiResponse code 200 và ShopResponse đã lưu |

ShopResponse gồm id, ownerId, shopName, description, logoUrl, bannerUrl, status, createdAt; trường null có thể được bỏ. Logo/banner chỉ đọc cấu trúc dữ liệu sẵn có, chưa triển khai upload shop.

Controller có OpenAPI/Swagger và SELLER method security. Service kiểm tra lại user role/status trong database, lấy danh tính qua SecurityUtils và không bind ownerId/status từ request. Khóa bi quan user tuần tự hóa các yêu cầu cùng owner; unique owner/name trong schema hiện có là chốt bảo vệ cuối. Xung đột tên giữa hai seller được database phân xử và transaction thua rollback. Không cần thay schema trong chức năng này.

Frontend service gắn sessionRequestConfig để không gửi yêu cầu bằng tài khoản mới sau khi phiên thay đổi. Trang hủy áp dụng kết quả cũ theo phiên và thế hệ request, khóa nút khi đang lưu. Shop INACTIVE/BANNED vẫn hiện trạng thái hiện có và không mở thêm.

## Kiểm chứng

- `cd backend; .\mvnw.cmd clean test`: 158 bài được liệt kê, **157 pass, 1 skip, 0 failure/error**. Bài MySqlSchemaIntegrationTest dùng cổng dev 3307 được skip vì môi trường dev không chạy; các bài mở shop dùng H2 riêng với unique name không phân biệt hoa thường.
- Sáu integration tests mới kiểm tra identity/server status, GET riêng tư, xác thực/phân quyền/token cũ, validation, shop bị khóa, tên trùng, cạnh tranh cùng owner/tên và rollback.
- `cd frontend; npm test`: **35/35 pass**, gồm bốn service tests mới. `npx tsc --noEmit` và `npm run build`: pass.
- `npm run test:e2e` với `E2E_BROWSER_CHANNEL=chrome`: **11/11 pass (52,4 giây)**, gồm bốn Playwright E2E mới trên Chrome, Nginx và backend/MySQL/Redis/RabbitMQ thật: đăng ký seller → mở shop → reload/relogin; lỗi trùng tên/quyền/riêng tư; tạo shop đồng thời và rollback trên MySQL; GET lỗi và phục hồi shop tạo ở tab khác. Các luồng mua hàng/OTP/refresh/upload/payment/email trước đó vẫn pass.
- Cụm thử riêng `helishop-integration`, web `http://localhost:18000`, API 18080; dùng Mailpit không relay và fixture email example.test. Không dùng dịch vụ thanh toán/email bên ngoài.

## Phần để sau

Chưa có duyệt shop, nâng role CUSTOMER, sửa shop/logo/banner, CRUD sản phẩm/SKU, quản lý đơn bán hay dashboard seller/admin. Theme hiện có được giữ. Không triển khai chức năng tiếp theo trước khi người dùng yêu cầu.
