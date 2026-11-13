# Chức năng 05 — Checkout, báo giá và nhiều shop

Ngày: 13/11/2026. Nhánh: `feat/server-checkout-and-pricing`. Chức năng 04 đã merge PR #4 (`ec88bc0`).

- POST `/orders/quote` nhận địa chỉ thuộc tài khoản, SKU/số lượng, phương thức giao/thanh toán, voucher và ghi chú. Không nhận tổng tiền do frontend tính.
- Giá SKU, trạng thái sản phẩm/shop, tồn kho và điều kiện voucher đều được kiểm tra ở server. Phí giao mỗi shop: tiêu chuẩn 30.000₫, nhanh 50.000₫, tiết kiệm 15.000₫. Voucher giới hạn shop, thời hạn/lượt dùng/tối thiểu/giảm tối đa được áp dụng; mã FREESHIP mẫu chỉ giảm trong phạm vi phí giao.
- POST `/orders/checkout-batch` yêu cầu `Idempotency-Key` 16–80 ký tự. Khóa khách hàng trước khi kiểm tra receipt; cùng key/nội dung trả lại đơn cũ, khác nội dung bị từ chối. Receipt lưu trong bảng checkout_receipts có unique(customer_id, request_key).
- Khóa voucher và SKU PESSIMISTIC_WRITE theo thứ tự ID, rồi khóa product. Toàn bộ đơn theo từng shop, tồn kho và lượt voucher ghi trong một giao dịch DB.
- Sau commit mới trừ số lượng đã mua trong giỏ Redis bằng Lua; giữ số lượng mới thêm đồng thời. Dọn giỏ lỗi không biến đơn đã lưu thành thất bại; UI hướng dẫn kiểm tra đơn trước khi đặt lại.
- Checkout dùng sổ địa chỉ thật, báo giá server, xác nhận mã đơn thật và thanh toán VNPAY từng đơn qua orderCode. Không còn ID ngẫu nhiên, tự gán PAID, phí/voucher cục bộ hoặc fallback success. Giỏ chỉ hiển thị tiền hàng, ưu đãi tính ở checkout.
- Bổ sung thông tin shop/SKU/ảnh trong OrderResponse và validation quantity > 0 cả luồng mới lẫn luồng checkout tương thích.

Kiểm chứng đã đạt: frontend **18/18 tests và production build pass**, backend **130/130 tests pass**, gồm test DB riêng chứng minh tách đơn, 4 request trùng đồng thời chỉ trừ kho một lần và rollback kho/đơn khi DB lỗi, cùng test API cho envelope, danh tính, header chống trùng và validation. DB tests dùng H2 riêng; nghiệm thu MySQL/Redis thật và trình duyệt nằm ở bước E2E.

Tiếp theo: lịch sử đơn/hủy đơn dùng API và khóa vòng đời, sau đó hoàn thiện return/IPN VNPAY.
