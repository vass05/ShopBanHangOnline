# Chức năng 06 — Lịch sử đơn và hủy đơn

Ngày: 13/11/2026. Nhánh: `feat/order-history-and-cancellation`. Chức năng 05 đã merge PR #5 (`568b53f`).

- GET `/orders/me` lấy đơn bằng danh tính token. UI dùng OrderResponse thật, hiển thị orderStatus/paymentStatus độc lập, tổng cuối finalAmount và snapshot hàng/địa chỉ.
- Bỏ order store persist; tài khoản khác hoặc xuất phiên xóa cache ngay, response cũ không được phục hồi lịch sử. Đồng bộ sự kiện đổi phiên giữa các tab cho auth/cart/orders.
- Hủy đơn gọi PATCH status, chỉ cập nhật giao diện sau phản hồi server. Khách chỉ hủy PENDING/CONFIRMED chưa trả tiền; yêu cầu lặp khi đã CANCELLED trả lại trạng thái hiện tại.
- OrderLifecycleService khóa đơn PESSIMISTIC_WRITE, kiểm tra lại quyền và chuyển trạng thái trong giao dịch; đơn đã hủy/trả không được kích hoạt lại. Hoàn kho khóa toàn bộ SKU rồi product theo ID để tránh đảo thứ tự khóa với checkout.
- Mua lại gửi SKU/số lượng thật qua Cart API, không tạo productId hoặc tồn kho giả. Thất bại từng phần được báo để người dùng kiểm tra giỏ.
- Checkout tương thích một đơn được đánh dấu deprecated; gắn shop thật theo SKU, chặn trộn shop hoặc shopId không khớp. Frontend sử dụng checkout-batch.

Kiểm chứng: **132/132 backend tests**, **22/22 frontend tests**, TypeScript/production build pass. Frontend store/API tests kiểm tra tải đơn, hủy thành công, hủy lỗi và response sau logout. Test DB kiểm tra hủy đồng thời chỉ hoàn kho một lần, không kích hoạt lại đơn terminal và khách không tự đẩy trạng thái giao/huỷ đơn PAID.

Hoàn tiền qua nhà cung cấp không nằm trong chức năng này: không tự đánh dấu REFUNDED khi chưa có xác nhận hoàn tiền. Hoàn thiện payment return/IPN ở nhánh tiếp theo.
