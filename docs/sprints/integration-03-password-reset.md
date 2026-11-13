# Chức năng 03 — OTP khôi phục mật khẩu

Ngày: 13/11/2026. Nhánh: `fix/password-reset-otp`. Chức năng 02 đã merge PR #2 (`fdf19ff`).

- SecureRandom tạo OTP 6 chữ số; Redis chỉ giữ bản băm BCrypt trong 10 phút, mã gốc chỉ có trong email.
- Redis Lua giới hạn một lần gửi mỗi 60 giây, tối đa 5 lần/giờ; sau 5 lần đoán sai mã bị vô hiệu. Consume mã đúng bằng compare-and-delete để không dùng lại đồng thời.
- Không trả OTP qua API hoặc log. Email không gửi được trả 503 và vô hiệu mã vừa phát, không báo gửi thành công. Vượt giới hạn trả 429.
- Tài khoản không tồn tại/ngưng hoạt động nhận thông báo yêu cầu chung, không gửi email.
- OTP dùng một lần; đặt lại mật khẩu thu hồi refresh token của tài khoản. Frontend giữ nguyên nội dung mật khẩu mới, không tự trim.
- OpenAPI mô tả giới hạn và mã lỗi mới; validation OTP chỉ chấp nhận chữ số. Bỏ log nội dung refresh token khi thu hồi.

Kiểm chứng: backend **116/116 tests pass**, gồm 7 test mới cho email/hash, giới hạn, SMTP lỗi, tài khoản vắng, sai mã, consume và mã đã dùng. Redis/SMTP được mock trong unit test; cần kiểm chứng SMTP thật bằng cấu hình môi trường hợp lệ ở đợt E2E.
