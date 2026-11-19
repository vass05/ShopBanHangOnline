# HeliShop frontend

React 18, TypeScript, Vite. Hướng dẫn chạy toàn bộ và cấu hình nhà cung cấp: [README gốc](../README.md).

```powershell
npm ci
npm run dev
```

Web dev: http://localhost:5173. Vite chuyển tiếp `/api` và `/uploads` sang `http://127.0.0.1:8080` mặc định, giữ Host frontend để Spring xử lý đúng các POST cùng origin. `VITE_API_BASE_URL` giữ `/api/v1`; không cần trỏ trình duyệt trực tiếp sang cổng backend.

Chạy `..\scripts\Start-Local.ps1` từ thư mục gốc để có backend Docker **8080** và dữ liệu chính. Dùng `.env.development.local` chỉ khi cần đổi cổng; nếu còn override **18080** của integration trước đây, bỏ override hoặc đổi về `http://127.0.0.1:8080`. Khởi động lại Vite sau khi đổi env nếu chưa tự nạp.

`DEV_API_PROXY_TARGET` theo cổng backend trên máy bạn. Với backend local 8080, bỏ override hoặc đặt `http://127.0.0.1:8080`. Sau khi đổi file env, khởi động lại Vite nếu nó chưa tự nạp lại. Cổng 5173 bị chiếm sẽ báo lỗi rõ ràng thay vì tự nhảy sang cổng khác.

`DEV_API_PROXY_TARGET` chỉ dành cho server Vite, không nhúng vào JavaScript trình duyệt. Docker vẫn dùng Nginx nội bộ; `VITE_API_BASE_URL` trong image là build arg, đổi giá trị đó cần build lại image.

```powershell
npm test
npm run build
```

Kiểm thử browser dùng dịch vụ thật của cụm Docker riêng:

```powershell
cd ..
.\scripts\Prepare-Integration.ps1
cd frontend
npx playwright install chromium
npm run test:e2e
```

Có Chrome trên Windows: đặt `$env:E2E_BROWSER_CHANNEL='chrome'` để dùng trình duyệt đã cài. Báo cáo nằm trong `playwright-report/`, ảnh/trace khi lỗi trong `test-results/`; cả hai được gitignore.

Các ca E2E tạo địa chỉ `@gmail.com` duy nhất nhưng thư được chặn trong Mailpit của integration, không gửi Gmail bên ngoài. Các ca thử dùng SKU 9001/9002 và chỉ thao tác container `helishop_integration_*`. SMTP gửi vào Mailpit, callback thanh toán dùng secret thử; không xác nhận được việc vận hành merchant/SMTP/Cloudinary bên ngoài.

Đăng ký mới bắt buộc OTP Gmail trước khi tạo tài khoản. Số điện thoại chỉ ở sổ địa chỉ nhận hàng. Admin dùng `/admin/users` để quản lý buyer/seller. Cấu hình SMTP thật nằm trong `.env.gmail.local` ở thư mục gốc; xem README gốc.
