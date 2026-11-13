# Chức năng 02 — Phiên đăng nhập và refresh token

Ngày: 13/11/2026. Nhánh: `fix/auth-session-persistence`. Chức năng 01 đã merge qua PR #1 (`9c1db40`).

- Chỉ lưu phiên vào localStorage nếu ghi nhớ; còn lại dùng sessionStorage, gồm cả metadata người dùng và token. Loại bỏ bản sao Zustand persist gây lưu token ngoài ý muốn.
- Đồng bộ Zustand qua thay đổi session; refresh token cập nhật ngay token hiển thị trong store. Đăng xuất xóa phiên ngay cả khi API lỗi.
- Migration chỉ nhận phiên cũ có cặp token còn tồn tại và khớp metadata, không khôi phục phiên đã đăng xuất từ dữ liệu persist cũ.
- Chỉ refresh request nghiệp vụ, không refresh lỗi đăng nhập/OTP. Nhiều lỗi 401 chờ một refresh, mỗi request chỉ retry một lần, hỗ trợ 401 đến trễ sau refresh.
- Refresh đang chạy không được ghi đè hoặc đăng xuất tài khoản mới; request của phiên cũ không được retry dưới tài khoản khác.
- Sau đăng nhập trở về đường dẫn redirect nội bộ hợp lệ.

Kiểm chứng: **10/10 frontend tests pass**, TypeScript và production build thành công. Test chạy module session thực và Axios interceptors thực với transport giả lập, không phụ thuộc cổng thanh toán hay máy chủ đang chạy.
