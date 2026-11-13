# Chức năng 07 — VNPAY và kết quả thanh toán

Ngày: 13/11/2026. Nhánh: `fix/vnpay-return-and-ipn`. Chức năng 06 đã merge PR #6 (`5ed68bf`).

- Tạo URL khóa đơn, kiểm tra chủ sở hữu/phương thức/trạng thái/số tiền, ghi PENDING và hạn 15 phút. Không tạo URL cho COD/đơn đóng/đã trả tiền. Khách không hủy trong phiên thanh toán chưa hết hạn; seller/admin không chuyển đơn VNPAY chưa PAID sang xử lý/giao hàng.
- Return URL mặc định dev là `http://localhost:5173/payment-result`. Trang này gửi tham số về `/payments/vnpay-return` để xác minh chữ ký, merchant, mã đơn và số tiền. Return chỉ đọc; đơn chưa có IPN vẫn PENDING, không tự đánh dấu PAID.
- UI lấy tiền cuối từ đơn server bằng VND, không tin `status=success`, amount, mã giao dịch mẫu hoặc vnp_Amount thô trên query. Poll tối đa 30 giây khi chờ IPN và có nút kiểm tra lại/đơn mua. Chưa đăng nhập thì chuyển đăng nhập với redirect nguyên vẹn.
- IPN khóa đơn PESSIMISTIC_WRITE, kiểm tra cả response code và transaction status, từ chối số tiền malformed/sai, kiểm tra giao dịch trùng và không ghi lại callback thất bại trùng. Callback trả đúng JSON riêng `{RspCode, Message}`.
- Thanh toán đến sau hủy vẫn được ghi vào ledger PAID để đối soát, giữ đơn đóng/kho đã hoàn và không phát sự kiện xử lý hàng. UI nêu rõ cần hỗ trợ hoàn tiền; không giả định đã REFUNDED.
- Sự kiện đơn trả tiền được lưu cùng giao dịch DB trong bảng `order_paid_outbox` (unique order_id). Worker mặc định 5 giây lấy tối đa 20 record, khóa từng ID trong giao dịch riêng và đợi RabbitMQ publisher confirm. Chỉ đánh dấu tuyến email/seller sau xác nhận; broker lỗi giữ record để thử lại.
- Outbox giảm mất sự kiện khi broker lỗi và tránh gửi lại tuyến đã xác nhận khi tuyến khác lỗi. Cơ chế giao nhận là **at least once**: crash sau broker confirm trước DB commit vẫn có thể phát lại; không tuyên bố gửi SMTP đúng một lần tuyệt đối.

Kiểm chứng: **144/144 backend tests**, **26/26 frontend tests**, production build pass. Test DB chứng minh IPN đồng thời chỉ tạo một transaction/một outbox, callback thất bại trùng không thêm ledger, Return không cập nhật PAID trước IPN. Test worker kiểm tra broker lỗi vẫn giữ sự kiện và gửi lại khi hoạt động.

Nguồn giao thức: [Tài liệu kết nối VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), [bảng mã phản hồi](https://sandbox.vnpayment.vn/apis/docs/bang-ma-loi/). Tách Return và IPN, kiểm tra hai mã trạng thái và chuẩn hóa số tiền được đối chiếu với tài liệu nhà cung cấp.

Chưa kiểm chứng giao dịch ngân hàng sandbox với merchant credential thật hoặc SMTP thật. Các dịch vụ nội bộ thật sẽ được nghiệm thu trong cụm E2E riêng; cần cấu hình `VNP_RETURN_URL` đúng origin frontend và đăng ký IPN công khai khi dùng nhà cung cấp.
