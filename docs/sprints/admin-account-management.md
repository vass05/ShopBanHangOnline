# Admin quản lý tài khoản — 19/11/2026

- Nhánh riêng `feat/admin-account-management`, tiếp sau PR #17 Gmail OTP. `/admin/users` là màn quản lý thật, không dùng dashboard giả lập: tìm email/họ tên, lọc buyer/seller và trạng thái, phân trang, xác nhận khóa/mở khóa, trạng thái tải/lỗi/thử lại/xung đột.
- GET `/api/v1/admin/users` và PATCH `/{id}/status` chuẩn ApiResponse/OpenAPI. Admin phải đang ACTIVE trong database; người mua/người bán không được gọi. DTO không trả password hash, token, địa chỉ hoặc số điện thoại.
- Chỉ quản lý CUSTOMER/SELLER, không khóa chính mình/admin khác, không đổi vai trò hay xóa tài khoản. Dữ liệu đơn, shop và liên hệ giao hàng giữ nguyên. Mở khóa đưa về ACTIVE, không tự thay trạng thái shop BANNED/INACTIVE.
- Khóa bản ghi người dùng, `expectedStatus` chống cập nhật từ dữ liệu cũ; thao tác lặp lại cùng trạng thái không lặp tác dụng phụ. Ghi log actorId/targetId/trạng thái, không log bí mật.
- `auth_version` (bigint default 0) và JWT claim cùng tên logic `authVersion`: thay trạng thái tăng version, xóa refresh token. JWT filter kiểm tra email/ID/trạng thái/version và lấy quyền từ database trên mỗi request, nên khóa có hiệu lực ngay, mở khóa không hồi sinh access token cũ. Token legacy thiếu version được xem là 0 khi tài khoản chưa đổi trạng thái.
- Migration chỉ thêm cột, không sửa email/mật khẩu/tài khoản có sẵn. Sao lưu SQL local trước thay đổi ở `.integration/backups/` (không commit). Dữ liệu cũ có sẵn một ROLE_ADMIN; không thêm admin công khai hay mật khẩu admin mặc định mới.
- Login admin chuyển thẳng `/admin/users`; menu tài khoản có liên kết quản lý. API mới ràng buộc phiên, UI bỏ response cũ khi đổi tài khoản và kiểm tra lại phiên sau hộp xác nhận.

Kiểm chứng local: 201 backend tests pass + 1 schema MySQL dev skip (database chính đã dừng để không thay schema qua test), 50/50 frontend tests và production build pass; 2/2 Chrome E2E quản lý/khóa/mở khóa/từ chối quyền trên Docker/MySQL/Redis thật. Full browser suite chạy trong CI của PR.
