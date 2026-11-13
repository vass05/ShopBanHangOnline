# Chức năng 08 — Catalog và phân trang server

Ngày: 13/11/2026. Nhánh: `feat/server-catalog-pagination`. Chức năng 07 đã merge PR #7 (`79179c9`).

- Trang chủ gửi keyword/category/shop/khoảng giá/rating/sắp xếp và page bắt đầu từ 1 tới backend, dùng totalElements/totalPages cho phân trang; không lọc trên 50 dòng đầu.
- Bộ chuyển đổi ProductDto dùng chung cho trang chủ và PDP, danh mục giữ cây ở mọi cấp. Header autocomplete truy vấn sản phẩm hiện có; sidebar render danh mục đệ quy.
- Không tạo SKU thay thế, giá gốc phần trăm, lượt bán/đánh giá/shop metrics hoặc badge xác thực khi API không cung cấp. Thiếu giá/ảnh dùng thông báo và placeholder trung tính; tồn kho 0 giữ nguyên, biến thể không khớp không được mua.
- Bỏ bộ lọc Flash Sale/Freeship/Mall và sort bán chạy chưa có backend hỗ trợ; phí/voucher hiển thị đúng ở checkout, không quảng cáo mã giả.
- Backend giới hạn page/size và trường sort; public catalog chỉ gồm sản phẩm/shop đang hoạt động. Chi tiết sản phẩm ngừng bán chỉ seller sở hữu/admin được xem.
- List response là bản nhẹ với mainImageUrl, shop/category và collections rỗng. Detail tải SKU/ảnh trong transaction; không fetch hai bag collections cùng một query.
- Checkout/hủy/trả phát StockChanged, xóa cache product sau commit để PDP phản ánh kho mới; checkout tương thích xóa cache sau khi tạo đơn thành công.

Kiểm chứng: **149/149 backend tests**, **30/30 frontend tests**, production build pass. Test DB tìm đúng 55 sản phẩm qua trang 3, không tải collections và tối đa 2 query; test detail có 2 SKU/2 ảnh; test cache kiểm tra stock 10 → 9 sau checkout → 10 sau hủy. Test API kiểm tra phân trang/sort sai và quyền xem hàng ngừng bán. Test frontend giữ stock 0, không bịa SKU/metrics, giữ cây sâu và gửi query server.
