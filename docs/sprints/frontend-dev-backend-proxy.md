# Sửa frontend dev không kết nối backend — 19/11/2026

Nhánh `fix/frontend-dev-backend-proxy`, baseline `3860291` (PR #15), bàn giao qua PR vào main.

## Nguyên nhân và thay đổi

Frontend dùng `npm run dev` ở 5173. Trang HTML trả 200 nhưng `/api/v1/products` trả 500 vì Vite chuyển tiếp tới localhost:8080 không có backend; backend Docker thực tế khỏe ở 127.0.0.1:18080. API qua Nginx 18000 vẫn trả 200.

Vite nay đọc biến server-only `DEV_API_PROXY_TARGET` từ env theo mode, mặc định http://127.0.0.1:8080. Máy hiện tại tạo frontend/.env.development.local với target http://127.0.0.1:18080; file này được gitignore và không đưa lên remote. Template .env.development.local.example và README hướng dẫn chọn cổng, quay lại 8080 và nạp lại Vite. strictPort giữ 5173, không tự nhảy cổng.

Browser tiếp tục gọi `/api/v1` cùng origin. `/api` và `/uploads` dùng chung target, giữ Host frontend như Nginx để Spring nhận đúng POST cùng origin. Không nới danh sách CORS đang chỉ cho 18000, không nhúng địa chỉ backend riêng vào JavaScript trình duyệt, không đổi backend/Docker port hay volume.

## Kiểm chứng

- Node tests **46/46 pass**, gồm test mới khởi động Vite từ config thật trên cổng ngẫu nhiên và backend HTTP tạm. Kiểm tra đường dẫn/query API, POST có Origin phải giữ Host/body, và ảnh upload chuyển tiếp đúng content type/bytes. Backend tạm từ chối Origin/Host khác nhau để phát hiện hồi quy CORS.
- Production build/TypeScript pass; Docker vẫn dùng cấu hình Nginx hiện có.
- Chrome thật trên localhost:5173: tài khoản thử example.test riêng đăng ký HTTP 201; đăng nhập, catalog, hồ sơ HTTP 200; 0 failed API requests. Không in token, không gửi email hoặc sửa dữ liệu người dùng cũ.
- Backend `mvnw.cmd test`: 184 bài liệt kê, **183 pass, 1 schema MySQL dev skip**, không failure/error.

Để dùng: sao chép template thành .env.development.local khi backend ở 18080; để mặc định hoặc đổi override về 8080 khi chạy backend local. Không dùng biến DEV_API_PROXY_TARGET làm VITE_API_BASE_URL: biến thứ nhất chỉ thuộc server Vite, biến thứ hai giữ /api/v1 trong browser.

Tham khảo [Vite loadEnv](https://vite.dev/config/#using-environment-variables-in-config) và [server.proxy](https://vite.dev/config/server-options#server-proxy).
