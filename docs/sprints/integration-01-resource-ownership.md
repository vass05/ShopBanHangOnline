# Chức năng 01 — Định danh và quyền sở hữu

Ngày: 13/11/2026. Nhánh: `fix/identity-and-resource-ownership`.

- Danh tính lấy từ JWT đã xác minh; customerId tương thích chỉ được trùng với người đang đăng nhập, không còn quyền ghi đè danh tính từ query/body.
- Giỏ hàng và tạo URL thanh toán sử dụng kiểm tra danh tính chung. Checkout không còn bắt buộc gửi customerId.
- Đọc đơn theo ID/mã: chỉ khách sở hữu, seller của shop hoặc admin. Danh sách theo customer: chính người đó hoặc admin.
- Khách chỉ hủy đơn của mình khi PENDING/CONFIRMED; không được tự đẩy đơn sang DELIVERED. Seller chỉ quản lý đơn/shop của mình.
- Tạo/sửa sản phẩm kiểm tra chủ shop; chặn đổi shop qua request sửa sản phẩm.
- Public registration không được cấp ROLE_ADMIN.
- Giữ ApiResponse và cập nhật mô tả OpenAPI; thêm 6 test tích hợp bảo vệ các đường truy cập vượt quyền.

Kiểm chứng: `.\mvnw.cmd test` — **109 tests, 0 failures, 0 errors, 0 skipped**. Các fixture của test hợp đồng cũ được cập nhật để có danh tính hợp lệ; test ownership mới dùng SecurityFilterChain, JWT và ResourceAccess thật.

Phạm vi tiếp theo: phiên frontend, OTP, ràng buộc số lượng và vòng đời đơn/khóa chống hoàn kho đồng thời được triển khai ở các nhánh chức năng riêng.
