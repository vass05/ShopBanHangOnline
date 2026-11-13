# Chức năng 04 — Giỏ hàng Redis

Ngày: 13/11/2026. Nhánh: `feat/redis-cart-integration`. Chức năng 03 đã merge PR #3 (`c71c646`).

- Giỏ đăng nhập gọi GET cart, POST items, PUT quantity, DELETE items/clear; dùng giá, tồn kho, tên sản phẩm và nhóm shop từ backend, thêm skuCode vào DTO backend.
- Không persist giỏ tài khoản vào localStorage; đổi tài khoản/xuất phiên xóa trạng thái ngay và bỏ qua response cũ. Request đang chờ không được gửi bằng token của phiên khác.
- Giỏ khách dùng khóa riêng. Đăng nhập ghép từng SKU theo số lượng lớn nhất của hai giỏ, không cộng dồn, để retry không nhân đôi sản phẩm. Chỉ xóa SKU khỏi giỏ khách sau response thành công; lỗi vẫn giữ giỏ khách để retry.
- Lựa chọn mua tách khỏi dữ liệu server; badge tính tổng quantity. Thao tác được xếp hàng để tránh response trả sai thứ tự.
- Cart/PDP có trạng thái chờ/lỗi; không hiện toast thêm thành công hoặc chuyển checkout nếu API thất bại. Bỏ debounce gây lệch số lượng khi chuyển checkout; giữ redirect sau đăng nhập.
- Không nhập dữ liệu từ khóa giỏ cũ không gắn tài khoản vì có thể thuộc tài khoản khác.

Kiểm chứng: frontend **15/15 tests pass**, TypeScript/production build pass. Năm test mới chạy store và Axios thật với adapter giả lập, bao gồm reload, giá server, ghép giỏ, API lỗi và đổi tài khoản khi request đang chạy. Backend giữ Redis HASH, TTL 30 ngày và nhóm shop hiện có; checkout thật được triển khai ở chức năng tiếp theo.
