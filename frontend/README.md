# HeliShop frontend

React 18, TypeScript, Vite. Hướng dẫn chạy toàn bộ và cấu hình nhà cung cấp: [README gốc](../README.md).

```powershell
npm ci
npm run dev
```

Web dev: http://localhost:5173. Vite chuyển tiếp `/api` và `/uploads` sang backend 8080. `VITE_API_BASE_URL` mặc định `/api/v1`; Docker dùng build arg cùng tên, đổi giá trị cần build lại image.

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

Các ca E2E tạo tài khoản `@example.test`, dùng SKU 9001/9002 và chỉ thao tác container `helishop_integration_*`. SMTP gửi vào Mailpit, callback thanh toán dùng secret thử; không xác nhận được việc vận hành merchant/SMTP/Cloudinary bên ngoài.
