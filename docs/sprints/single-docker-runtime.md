# Một cụm Docker chính — 19/11/2026

- Nhánh `chore/single-docker-runtime`, sau Gmail OTP và quản lý tài khoản admin.
- `docker-compose.yml` dùng project cố định `webbnhngonline`, 5 service frontend/backend/MySQL/Redis/RabbitMQ. Frontend http://localhost; backend 8080; Vite dev 5173 proxy về 8080. Các cổng chỉ bind loopback. Không thêm Mailpit vào môi trường chính.
- Volume giữ đúng tên cũ `webbnhngonline_mysql_data`, `_redis_data`, `_rabbitmq_data`; ảnh đại diện dùng thư mục `backend/uploads` đã có. Network cũng giữ tên cũ. Trước khi chuyển đã sao lưu cả hai database trong `.integration/backups/` bị Git bỏ qua.
- `scripts/Start-Local.ps1` tạo `.env` chứa JWT secret một lần, giữ hostname RabbitMQ từ container cũ để không thay node identity trong dữ liệu Mnesia. Không tái sinh mật khẩu cho volume có sẵn. Khởi động lại bằng `-NoBuild` sau khi sửa SMTP.
- Script chỉ dừng container thuộc project integration đang chạy; không chạy `down -v`, không prune, không xóa container/volume/database, không chạy seed/fixture. Không tác động project smartclassroom. Integration giữ nguyên để dùng khi chủ động kiểm thử; CI tự tạo stack riêng trên runner.
- `.env.gmail.local` được backend nạp qua env_file. Spring nhận SMTP_* và vẫn ưu tiên SPRING_MAIL_* cho production/integration. Bỏ thông tin Gmail mẫu giả, yêu cầu STARTTLS khi dùng Gmail; integration vẫn SMTP nội bộ không TLS qua override riêng. File chứa bí mật không vào Git hoặc image.
- Dữ liệu chính đã có 8 user, 8 product, 0 order và 1 admin trước chuyển. Không nhập dữ liệu kiểm thử vào database này. `auth_version` được Hibernate thêm với default 0, không reset tài khoản.
- Hướng dẫn mặc định trong README thống nhất một stack + tùy chọn npm dev; production và integration là công cụ dành cho triển khai/kiểm thử, không phải hai stack cần bật hằng ngày. Không nạp seed vào dữ liệu có sẵn.

Kiểm chứng local: 5/5 service healthy, đúng một project HeliShop đang chạy, integration đã dừng. Dữ liệu sau chuyển vẫn 8 user/8 product/0 order/1 admin. Catalog qua localhost, 5173 và 8080 trả 200; admin mẫu có sẵn đăng nhập và đọc 7 tài khoản quản lý thành công. Chrome kiểm tra cả Docker/Vite đều tới màn admin thật, 0 JavaScript error. Compose config và PowerShell parser pass; TypeScript noEmit pass. Backend suite 202/202 pass, không còn cảnh báo compiler. Khai báo MapStruct bỏ qua authVersion nội bộ và bỏ import test dư được IDE cảnh báo; không tắt chẩn đoán. VS Code đã xác nhận No Problems, Java Ready.

SMTP thật vẫn chờ người dùng điền hai trường trong `.env.gmail.local`; không gửi thư thử ra Gmail khi cấu hình trống. Các kiểm thử OTP/SMTP và toàn bộ 22 browser E2E đã pass CI của PR admin. Nhánh Docker sẽ chạy lại CI đầy đủ.
