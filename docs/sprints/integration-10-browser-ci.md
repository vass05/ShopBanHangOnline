# Chức năng 10 — Browser E2E và CI

Ngày: 13/11/2026. Nhánh: `test/browser-checkout-and-ci`. Chức năng 09 đã merge PR #9 (`7753e1a`). Mỗi chức năng theo nhánh → PR → merge main như người dùng yêu cầu.

## Thay đổi

- Thêm Playwright, cấu hình một worker, fail nếu test.only trên CI, không retry để che lỗi; giữ screenshot/trace khi lỗi và báo cáo HTML. Test chỉ chấp nhận web localhost/127.0.0.1 cổng 18000 của cụm riêng.
- `scripts/Prepare-Integration.ps1` tạo cấu hình riêng từ template, kiểm tra project/container/volume dành riêng, build và đợi health, nạp hai shop/SKU test. Không dùng seed chứa tài khoản cá nhân, không đụng dữ liệu dev/production. File fixture không dùng để khôi phục production.
- `docker-compose.e2e.yml` thêm Mailpit SMTP nội bộ và merchant/secret thử. Không bật relay mail hay thực hiện giao dịch ngân hàng. Fixture seller không có mật khẩu đăng nhập dùng được; khách đăng ký API mới mỗi ca.
- Kiểm thử dịch vụ thật phát hiện SMTP không có timeout làm treo worker khi mail server tắt. Thêm timeout kết nối 3 giây, đọc/ghi 5 giây. Listener DLQ mặc định dừng để broker giữ thông điệp lỗi cho vận hành, tránh ACK mất thư sau khi chỉ ghi log.
- CI có ba job backend/frontend/browser trên PR và main; pin commit của Actions chính thức, Java 21/Node 24, quyền contents:read, giữ báo cáo 7 ngày, thu log khi lỗi và dừng cụm của runner. Không triển khai máy chủ công khai hoặc yêu cầu secret thật trên CI.
- README gốc và frontend được cập nhật bằng hướng dẫn dự án thay cho template, nêu đúng cách khởi động, cấu hình build/runtime, E2E và giới hạn nhà cung cấp.

CI trên runner sạch phát hiện Nginx gửi Host không kèm cổng, làm Spring coi POST login là cross-origin và trả 403. Đã tái hiện bằng 127.0.0.1:18000, sửa proxy giữ nguyên `Host $http_host` cho cả API/uploads/Swagger, thêm expectation HTTP login và ca hồi quy host không nằm trong CORS allowlist. Không mở rộng CORS wildcard để che lỗi.

## Kiểm chứng

**152/152 backend tests, 31/31 frontend tests, production build pass. 7/7 browser E2E pass trên Chrome headless Windows với MySQL/Redis/RabbitMQ/Nginx/Mailpit thực tế:**

1. OTP đến SMTP nội bộ, không xuất hiện trong API, cooldown 429, dùng một lần, mật khẩu cũ và refresh cũ bị thu hồi, đăng nhập UI bằng mật khẩu mới.
2. Checkout hai shop, phí từng shop, lặp receipt giữ nguyên ID; tạo URL theo orderCode/số tiền thật. Return không tự ghi PAID; chữ ký sai/số tiền sai bị chặn. Bốn IPN đồng thời chỉ một lần thành công, một ledger và một outbox; UI cập nhật sau IPN, invoice đến Mailpit qua RabbitMQ và cả hai route outbox được broker xác nhận.
3. Upload avatar từ UI, ảnh giải mã được trong trình duyệt qua Nginx; ảnh giả bị 400, quá 5 MB bị 413. Tạo lại riêng backend, nội dung ảnh giữ nguyên và UI tải lại được.
4. Mua COD từ PDP → cart → checkout → lịch sử: Redis HASH với TTL 30 ngày, trừ kho server, người khác không đọc đơn; hủy/đọc lại/hủy lặp chỉ hoàn kho một lần và tồn kho cache phản ánh đúng.
5. Token truy cập sai được refresh qua backend thật; phiên không rememberMe nằm trong sessionStorage, tab độc lập không đăng nhập; logout xóa phiên và refresh token cũ không dùng được.
6. Đăng nhập qua 127.0.0.1:18000 vẫn thành công dù allowlist chỉ có localhost; proxy giữ đúng origin và cổng.
7. Dừng Mailpit của cụm thử, gửi IPN hợp lệ: đơn vẫn PAID, một ledger; worker retry và thông điệp giữ trong DLQ. Mailpit được bật lại trong finally. Không tự gửi lại thông điệp DLQ để tránh email lặp ngoài ý muốn.

CI được kích hoạt khi tạo PR; trạng thái chạy được xem ở checks của PR và trang Actions. Test schema MySQL ở cổng dev 3307 có điều kiện và có thể skip trên runner; browser job kiểm chứng MySQL của cụm riêng qua cổng nội bộ Docker.

## Phạm vi bàn giao và phần tiếp theo

Đã nghiệm thu tích hợp cục bộ cho luồng khách hàng theo kế hoạch. Bộ backend vẫn bao gồm khóa bi quan, cạnh tranh checkout/hủy/IPN, rollback, voucher và quyền truy cập. Các kiểm thử không phải chứng nhận chịu tải production.

Chưa nghiệm thu merchant ngân hàng/SMTP/Cloudinary thật. Trước phát hành cần cấu hình callback HTTPS công khai, chứng chỉ/domain, backup/khôi phục volume, migration schema thay `ddl-auto=update` và quy trình quan sát/gửi lại DLQ. Giao nhận email theo at least once, có thể trùng nếu worker dừng giữa SMTP thành công và ACK. Seller-notify queue đang giữ sự kiện để tích hợp kênh thông báo seller ở đợt sau.

Tài liệu tham khảo: [Playwright configuration](https://playwright.dev/docs/test-configuration), [Playwright CI](https://playwright.dev/docs/ci), [Mailpit API](https://mailpit.axllent.org/docs/api-v1/), [Mailpit SMTP](https://mailpit.axllent.org/docs/usage/sending-messages/). Bằng chứng thực tế nằm trong báo cáo/log của lần chạy và checks CI; tài liệu nhà cung cấp không thay thế nghiệm thu.
