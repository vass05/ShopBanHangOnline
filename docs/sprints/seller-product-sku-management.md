# Quản lý sản phẩm và SKU seller — 16/11/2026

Nhánh `feat/seller-product-sku-management`, baseline `f4e79dc` (PR #12), bàn giao nhánh → kiểm thử → PR → main. Đây là chức năng tiếp sau mở gian hàng, theo yêu cầu người dùng; không triển khai đơn bán/admin hoặc đổi theme.

## Hành vi

Seller đã mở shop chọn **Quản lý sản phẩm & SKU** tại `/seller/shop`, hoặc vào `/seller/products`. Danh sách riêng shop có phân trang, tìm tên/mô tả và lọc trạng thái. Màn tạo lưu nguyên tử thông tin sản phẩm cùng SKU đầu tiên, mặc định bản nháp. Seller sửa tên/danh mục/mô tả/URL ảnh/trạng thái và quản lý SKU ngay trong chi tiết. Reload/đăng nhập lại đọc database; lỗi giữ form, danh sách lỗi có thử lại, version cũ có tải lại dữ liệu.

Giá sản phẩm bằng giá SKU thấp nhất, tổng kho bằng tổng tồn SKU. Khi có khách mua, version SKU tăng; form cũ không thể ghi đè lượng đã bán. Khi lưu không thay đổi dữ liệu thì version có thể giữ nguyên. **Đã ẩn** ngừng bán trên catalog, chi tiết công khai và cả hai API checkout; seller vẫn xem được sản phẩm của mình.

## API

Các route dưới có prefix `/api/v1/seller/products`, yêu cầu JWT SELLER và kiểm tra user/shop ACTIVE từ database. Server xác định shop, không nhận owner/shop/aggregate/rating từ form. API đóng gói ApiResponse và có OpenAPI.

| Method/route | Dữ liệu | Kết quả |
| --- | --- | --- |
| GET `/` | page (1-based), size ≤50, keyword, status | PageResponse chỉ chứa sản phẩm shop hiện tại, gồm draft/hidden |
| GET `/{id}` | ID sản phẩm | Chi tiết mới nhất, SKU/version, không dùng cache công khai |
| POST `/` | `{product: {name, categoryId, description?, mainImageUrl?, status}, skus: [...]}` | HTTP 201; lưu 1–100 SKU cùng sản phẩm trong transaction, slug tự sinh |
| PUT `/{id}` | Metadata như product ở trên | Sửa metadata/trạng thái, tính lại aggregate từ SKU |
| POST `/{id}/skus` | `{skuCode, price, originalPrice?, stockQuantity, attributes?, skuImageUrl?}` | HTTP 201; thêm SKU, trả product mới nhất |
| PUT `/{id}/skus/{skuId}` | Như trên và version bắt buộc | Sửa đúng SKU thuộc product/shop; stale version trả 409 |

Validation: tên ≤255; mô tả ≤10000; ảnh ≤500 và URL http/https hoặc /uploads/. Mã SKU trim/uppercase, duy nhất toàn hệ thống; giá dương, tối đa 13 chữ số nguyên/2 lẻ, giá gốc không thấp hơn giá bán; kho nguyên 0–1.000.000. Mỗi SKU tối đa hai thuộc tính, tên ≤50/giá trị ≤100, cùng tên thuộc tính và khác tổ hợp. Chưa có shop trả 404, sai role/shop khóa hoặc chéo shop trả 403, validation 400, trùng mã/tổ hợp hoặc stale version 409.

## Nhất quán và tương thích

Seller writes dùng READ_COMMITTED, khóa SKU theo ID tăng dần rồi parent product, cùng thứ tự checkout/cancellation. Unique SKU code xử lý cuộc đua giữa shop khác nhau; transaction thất bại rollback cả product và SKU. Flush trước khi trả để version phản ánh giá trị mới. ProductStockChanged xóa cache products sau commit.

API products cũ chỉ tạo draft không SKU; không publish trước khi thêm SKU. Update cũ từ chối price/stock của product có SKU và điểm đánh giá nhập tay. ResourceAccess kiểm tra trạng thái seller/shop trên đường ghi cũ. Checkout cũ khóa SKU trước parent và kiểm tra trạng thái ngừng bán. Giữ ID SKU, snapshot đơn và dữ liệu order cũ.

## Kiểm chứng

- Backend `clean test`: **169 bài liệt kê, 168 pass, 1 skip, 0 failure/error**. Schema MySQL dev test skip vì cổng 3307 không chạy. 11 integration tests mới kiểm tra DB/API quyền, atomicity, validation, duplicate/rollback, cache, ownership, stale version sau checkout và cạnh tranh cùng SKU/checkout.
- Frontend `npm test`: **40/40 pass**, gồm năm service tests mới về private list, whitelist payload, version, lỗi và chuyển phiên; `npm run build` kiểm tra TypeScript/project references và Vite thành công.
- Browser toàn bộ **14/14 pass**. Ba Playwright E2E mới trên Chrome/MySQL/Redis/RabbitMQ/Nginx thật: seller tạo/sửa/publish SKU → khách mua biến thể → giá/kho đúng → stale edit 409 → reload/restock → ẩn bán; lỗi tải/duplicate/form/quyền; MySQL cạnh tranh edit/checkout và hai biến thể khác nhau. Các luồng mua hàng/email/OTP/upload/phiên cũ vẫn pass.
- Java đã dọn cache sau Maven clean, xác nhận **No Problems / Ready**. File/package probe tạm báo đúng một lỗi có chủ đích, tự hết lỗi sau sửa rồi đã xóa; không giữ probe trong repository.
- Cụm thử `helishop-integration`, web 18000/API 18080/Mailpit 18025; không khởi động cụm dev song song. Dữ liệu fixture/volume tách biệt, không gửi email/ngân hàng bên ngoài.

## Giới hạn

Ảnh nhận URL, chưa upload ảnh sản phẩm/gallery. Không xóa cứng SKU đang được giỏ/đơn tham chiếu; dùng trạng thái sản phẩm hoặc kho 0 để ngừng bán. Chưa sửa đồng loạt schema thuộc tính nhiều SKU, import/export, đơn bán hay dashboard. Dừng tại chức năng này sau PR.
