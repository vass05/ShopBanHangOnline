# Giao diện đăng nhập và đăng ký — 19/11/2026

- Trang đăng nhập dùng chung khung với trang đăng ký. Form đặt email, mật khẩu, quên mật khẩu và duy trì đăng nhập trong cùng khung. Nút Google chính thức nằm dưới phần phân cách.
- Đăng ký Gmail bắt đầu với địa chỉ và nút Tiếp tục. Sau khi gửi mã thành công, người dùng nhập OTP, mật khẩu và vai trò. Người dùng có thể đổi địa chỉ hoặc gửi lại mã. Google xác minh Gmail, sau đó tài khoản mới chọn vai trò.
- Duy trì đăng nhập áp dụng cho cả Google và Gmail. Số điện thoại nằm ở địa chỉ nhận hàng.
- Bỏ hai tệp env Vite trùng với mặc định `/api/v1` và override local trỏ 8080. Vite tự gọi backend 8080. Cấu hình Google, SMTP, production và kiểm thử phục vụ các luồng tương ứng.
- Đã gỡ project kiểm thử khỏi Docker Desktop bằng Compose down. Bốn volume kiểm thử vẫn còn. Docker chính là project `webbnhngonline` với năm dịch vụ. GitHub Actions dựng môi trường kiểm thử riêng trên runner.

Kiểm tra tại máy local đạt 224 bài backend, 55 bài frontend, TypeScript và build. Chrome trên Docker và Vite đã kiểm tra đăng nhập admin, mở hộp thoại quên mật khẩu, chuyển bước khi gửi OTP và giữ đúng bước khi dịch vụ email trả lỗi. Hai phản hồi gửi OTP trong lần kiểm tra giao diện được mô phỏng. Bài E2E seller trong CI dùng SMTP kiểm thử.
