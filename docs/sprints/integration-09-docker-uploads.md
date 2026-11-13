# Chức năng 09 — Docker và ảnh upload

Ngày: 13/11/2026. Nhánh: `fix/docker-runtime-and-uploads`. Chức năng 08 đã merge PR #8 (`5bef5e3`).

- Frontend dùng `/api/v1` cùng origin qua Nginx hoặc Vite proxy. API origin là build arg; media helper dùng cùng origin đó khi cấu hình API riêng, bỏ URL localhost cố định trong avatar.
- Nginx proxy `/uploads/` có ưu tiên trước regex ảnh tĩnh. Backend dùng named volume `/app/uploads`, user không phải root UID 10001 và tạo sẵn thư mục có quyền ghi. JVM và MySQL cùng múi giờ Việt Nam.
- Avatar kiểm tra MIME/chữ ký định dạng JPEG/PNG/WEBP, tên UUID với đuôi suy ra từ nội dung, đóng stream; UI/server giới hạn 5 MB, request/Nginx 6 MB, phản hồi 413 khi quá cỡ.
- Compose nhận mật khẩu/JWT từ file riêng, script sinh ngẫu nhiên không ghi đè file cũ. Cổng, container và volume tùy chỉnh được; giữ mặc định tên volume dữ liệu trước đây, cổng file mẫu tránh dev.
- VNPAY mặc định khóa trong Compose khi chưa cấu hình merchant. SMTP/Cloudinary cấu hình ngoài image; CORS chỉ chấp nhận origin đã khai báo. Không tự gửi email ra ngoài khi chạy kiểm thử.
- README hướng dẫn lần đầu, bật lại, build lại, seed chỉ database demo, theo dõi health và giữ dữ liệu khi dừng.

Kiểm chứng tự động: **152/152 backend tests**, **31/31 frontend tests**, production build pass. Bao gồm ảnh giả, tên tệp traversal/đuôi HTML, tên duy nhất, quá 5 MB và media URL với API origin riêng.

Kiểm chứng Docker thực tế: build cả hai image, đủ **5/5 services healthy**. Smoke qua web 18000 xác nhận SPA deep link, catalog API, đăng ký, upload PNG với tên gốc `.html`, ảnh giả bị 400 và file trên 5 MB bị 413. Sau `up --force-recreate --no-deps backend`, ảnh giữ nguyên từng byte và vẫn tải được qua Nginx. App chạy UID/GID 10001, thư mục ảnh ghi được, giờ container +07. Healthcheck dùng IPv4 để tránh localhost IPv6; Nginx dùng Docker DNS có refresh để theo địa chỉ backend khi tạo lại. Tham khảo: [Nginx proxy_pass/resolver](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass).

Môi trường kiểm thử dùng project `helishop-integration`, web 18000, API 18080, volume riêng; các container dev `eshop_*` vẫn healthy, không bị thay đổi. Unit tests không thay thế nghiệm thu dịch vụ ngoài.

Phạm vi: chưa nghiệm thu giao dịch với merchant ngân hàng, SMTP thật hoặc Cloudinary thật. TLS công khai, backup và chuyển schema có kiểm soát vẫn cần cấu hình theo máy chủ đích.
