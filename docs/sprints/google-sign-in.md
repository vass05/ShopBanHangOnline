# Google Sign-In — 19/11/2026

Nhánh `feat/google-sign-in`, theo yêu cầu thay Google thành luồng chính; tiếp tục giữ email/mật khẩu/OTP cũ và một cụm Docker hằng ngày.

- Google Identity Services render nút chính thức trên login/register; cấu hình runtime từ backend. Không có client secret, không gửi email OTP riêng cho Google. Thiếu cấu hình báo chưa khả dụng, vẫn đăng nhập tài khoản cũ được.
- API `/auth/google/config`, `/challenge`, POST `/auth/google`, `/complete`: ApiResponse/OpenAPI, nonce cookie HttpOnly SameSite=Lax, thời hạn 5 phút, nonce/ticket một lần trong Redis. Ticket ràng buộc trình duyệt và chỉ giữ định danh đã xác minh, không giữ Google ID token hoặc mật khẩu.
- Nimbus JWT từ Spring Security kiểm tra chứng thư Google RS256, issuer/audience/azp/hạn/nonce/email_verified/Gmail. JWKS Google cố định, không cho môi trường production đổi sang issuer giả.
- User mới chỉ chọn CUSTOMER/SELLER sau xác minh; chưa cấp phiên hoặc tạo user trước khi hoàn tất. `google_subject` unique, mật khẩu nullable cho Google-only. Runner migration kiểm tra MySQL và chỉ bỏ NOT NULL của password_hash khi còn constraint cũ (Hibernate update không tự thay phần này); chạy lặp không sửa mật khẩu hoặc user. Không tạo mật khẩu mặc định/giả. Email/role/Google subject từ client không được dùng để thay định danh/quyền.
- User cũ phải nhập mật khẩu hiện tại để liên kết lần đầu, tối đa 5 lần sai/ticket. Giữ ID/role/password/profile/shop/đơn, kiểm tra ACTIVE và authVersion như đăng nhập cũ. Không tự liên kết hoặc cấp admin theo email. Khóa user vẫn có hiệu lực với Google/JWT cũ.
- Giao diện bảo vệ response cũ khi đổi phiên, kể cả trường hợp chưa đăng nhập → đăng nhập tài khoản khác → đăng xuất. Chặn callback/click trùng; hỗ trợ retry, ticket hết hạn, Google unavailable, role choice và link confirmation.
- Docker nạp `.env.google.local` (không commit), `Start-Local` giữ dữ liệu và một stack chính; production có GOOGLE_CLIENT_ID riêng. Người dùng chưa có Client ID, đã chuẩn bị tệp/hướng dẫn [thiết lập Google](../google-sign-in-setup.md).
- Sao lưu SQL dữ liệu chính trước migration trong `.integration/backups/` bị Git bỏ qua. Không seed/reset hoặc xóa volume.

Kiểm chứng local: 224/224 backend tests pass (gồm 8 RSA, 12 API/Redis và 2 migration tests); 55/55 frontend tests, TypeScript và build pass. Chrome 11/11 ca Google/seller/admin pass, gồm 5 ca Google UI/provider stub, có kiểm tra chống phản hồi cũ khi đổi phiên. Toàn bộ browser suite sẽ chạy trong CI. Không khẳng định đăng nhập Google thật đã hoạt động khi thiếu Client ID.
