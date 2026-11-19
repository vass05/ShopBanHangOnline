# Bật Đăng nhập bằng Google

Google là cách đăng ký/đăng nhập chính. Gmail được Google xác thực, không cần SMTP hoặc OTP HeliShop cho luồng này. Chủ website cấu hình một lần; người mua/người bán không cần điền cấu hình.

## 1. Tạo Web Client ID

1. Mở [Google Cloud Console](https://console.cloud.google.com/), tạo/chọn project HeliShop.
2. Vào **Google Auth Platform**. Trong **Branding**, điền tên ứng dụng và email hỗ trợ/liên hệ của bạn. Chọn **Audience** phù hợp; nếu dùng chế độ Testing và Google giới hạn truy cập, thêm Gmail của bạn vào danh sách Test users.
3. Vào **Clients → Create client → Web application**.
4. Thêm **Authorized JavaScript origins**:
   - `http://localhost`
   - `http://localhost:5173`
   - Khi triển khai thật, thêm origin HTTPS của website, không kèm đường dẫn. Nếu bạn mở bằng `127.0.0.1` thì phải khai báo origin đó riêng.
5. Sao chép **Client ID** có đuôi `.apps.googleusercontent.com`. Luồng GIS JavaScript callback này không cần Client Secret hoặc redirect URI riêng. Không cấp quyền đọc Gmail hoặc các quyền Drive/Calendar.

Tham khảo chính thức: [Google Identity Services Setup](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid).

## 2. Điền cấu hình local

Tệp `.env.google.local` đã được chuẩn bị ở thư mục gốc, bị Git bỏ qua:

```dotenv
GOOGLE_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

Chỉ điền Client ID của ứng dụng. Không điền mật khẩu Google, App Password hoặc Client Secret vào tệp này.

Sau khi code mới đã build, lưu file và chạy ở thư mục gốc:

```powershell
.\scripts\Start-Local.ps1 -NoBuild
```

Backend nạp Client ID và cung cấp cấu hình công khai cho frontend qua `/api/v1/auth/google/config`; không cần build lại frontend khi đổi Client ID. Lần đầu cập nhật code dùng `Start-Local.ps1` để build image mới.

Production dùng `GOOGLE_CLIENT_ID` trong `.env.production` rồi chạy lại Compose production. Không trỏ production tới khóa/issuer kiểm thử: verifier chỉ tin chứng thư Google và kiểm tra chữ ký, issuer, audience, hạn, nonce, Gmail đã xác minh.

## 3. Sử dụng

- Mở `http://localhost` hoặc `http://localhost:5173`, chọn **Tiếp tục với Google**.
- Người mới: Gmail được xác minh trước, sau đó chọn Người mua hàng/Người bán hàng và hoàn tất. Không yêu cầu mật khẩu HeliShop hoặc OTP riêng.
- Email đã có tài khoản: xác nhận mật khẩu HeliShop hiện tại một lần để liên kết. Giữ nguyên ID, vai trò, mật khẩu, shop/đơn và dữ liệu cá nhân; lần sau chỉ cần Google. Không tự liên kết admin chỉ dựa trên email.
- Tài khoản đã liên kết được xác định bằng Google `sub`, không dựa vào tên hoặc email do trình duyệt tự gửi. Tài khoản bị khóa tiếp tục bị từ chối.
- Admin mẫu cũ trong database local: `admin.helishop@gmail.com`, mật khẩu theo bộ seed `Password123!`; dùng form mật khẩu, chuyển tới `/admin/users`. Không cần đăng nhập được Google của địa chỉ mẫu để dùng admin local. Tài khoản thực tế đã đổi mật khẩu phải dùng mật khẩu mới.
- Số điện thoại chỉ nhập trong địa chỉ nhận hàng. Gửi hóa đơn/email OTP dự phòng vẫn cần cấu hình gửi thư riêng trong `.env.gmail.local`.

Khi chưa điền Client ID, nút Google hiển thị tạm chưa khả dụng; không giả lập đăng nhập thành công. Form mật khẩu cũ vẫn dùng được. Gmail + mật khẩu/OTP vẫn nằm trong phần tùy chọn trên trang đăng ký.

## Kiểm thử và giới hạn

Verifier có kiểm thử RSA thật cho token hợp lệ/sai chữ ký/issuer/audience/hạn/nonce/email. API được kiểm thử với H2, Redis thật ở logical DB 15 và Google identity được giả lập tại ranh giới provider. CI backend có Redis riêng, không dùng database chính. Không flush Redis.

Các ca UI tự động giả lập Google SDK và phản hồi provider, dùng phiên HeliShop kiểm thử hợp lệ; không đăng nhập Google thật, không có đường bỏ qua Google trong backend production. Kiểm chứng với tài khoản Google thật cần Client ID chính chủ và thao tác chọn tài khoản trên Google. [Quy tắc xác minh token chính thức](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token).
