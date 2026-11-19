# Gmail + mật khẩu và OTP đăng ký — 19/11/2026

- Đăng ký mới chỉ nhận `@gmail.com`, vai trò CUSTOMER/SELLER; admin không được tự đăng ký. `POST /auth/register/otp` gửi email trước, không tạo user. `POST /auth/register` bắt buộc OTP, chỉ tạo user/cấp token sau xác thực.
- OTP băm BCrypt trong Redis, TTL 10 phút; chờ 60 giây gửi lại, tối đa 5 lần/giờ và 5 lần sai. Lua so sánh hash và tiêu thụ nguyên tử, chống dùng lại/đăng ký đồng thời. SMTP lỗi trả 503 và vô hiệu mã. Không trả hoặc log OTP.
- Đăng nhập và khôi phục mật khẩu bỏ tra cứu số điện thoại. Email cũ tiếp tục đăng nhập được để giữ quyền truy cập dữ liệu/admin; không tạo thêm tài khoản email giả từ phone. Kiểm kê local không có tài khoản `@phone.helishop.com`.
- Bỏ số điện thoại trong form đăng ký và chỉnh hồ sơ; phone nằm trong từng địa chỉ nhận hàng, không phải định danh tài khoản. Không xóa trường legacy hoặc dữ liệu điện thoại cũ trong database.
- Tệp `.env.gmail.local` bị Git bỏ qua. Điền `SMTP_USERNAME` và `SMTP_PASSWORD` (Google App Password, không phải mật khẩu Gmail). Không đưa bí mật vào chat, commit, Dockerfile hoặc build args.
- Cấu hình SMTP: `smtp.gmail.com:587`, auth + STARTTLS. Nguồn chính thức: [Google SMTP](https://developers.google.com/workspace/gmail/imap/imap-smtp), [Google App Passwords](https://support.google.com/accounts/answer/185833). App Password yêu cầu bật xác minh hai bước và có thể không khả dụng với một số chính sách tài khoản.
- Kiểm thử email dùng Mailpit trong môi trường integration riêng; không chứng minh được thư đến Gmail thật. Cần điền cấu hình gửi thư trước khi dùng đăng ký thật.

Kiểm chứng local: 195/195 backend tests, 46/46 frontend tests + TypeScript/production build, 7/7 browser E2E liên quan. Bao gồm cuộc đua đăng ký cùng mã, không tạo user trước xác thực, hết lượt nhập sai, khôi phục mật khẩu và đăng ký seller qua UI. Full browser suite chạy trong CI trên volume mới của runner.
