# Seller xử lý đơn — 19/11/2026

Nhánh `feat/seller-order-fulfillment`, baseline `3cc17cc` (PR #14), bàn giao nhánh → kiểm thử → PR → main. Chức năng tiếp theo sau quản lý sản phẩm/SKU, giữ giao diện hiện có.

## Hành vi và hợp đồng

Trang `/seller/orders` nối từ shop/header/sản phẩm, chỉ có đơn của shop đang đăng nhập. Hỗ trợ tìm mã đơn, lọc trạng thái/thanh toán và phân trang. Chi tiết có snapshot hàng/biến thể/giá, thông tin người nhận/địa chỉ/điện thoại và ghi chú, phí/giảm/tổng tiền. Server trả `allowedStatuses` và `actionNote`; frontend chỉ hiển thị các thao tác này, xác nhận trước khi ghi, không đổi trạng thái giả khi request lỗi.

| API | Dữ liệu | Kết quả |
| --- | --- | --- |
| GET `/api/v1/seller/orders` | page 1-based, size ≤50, keyword ≤64, status, paymentStatus | PageResponse riêng shop; items rỗng ở bản tóm tắt, không fetch collection khi phân trang |
| GET `/api/v1/seller/orders/{id}` | ID đơn | `{order: OrderResponse, allowedStatuses, actionNote?, updatedAt}` |
| PATCH `/api/v1/seller/orders/{id}/status` | `{expectedStatus, status}` | Cùng cấu trúc chi tiết với trạng thái và thao tác mới nhất |

Các endpoint có ApiResponse/OpenAPI, JWT SELLER; shop lấy từ user đăng nhập và kiểm tra role/status database cùng shop ACTIVE. Không nhận shopId/customerId/paymentStatus để ghi. Customer/admin không dùng API seller; endpoint quản trị cũ giữ quyền riêng. Không cho đọc/ghi chéo shop.

## Trạng thái và thanh toán

- PENDING → CONFIRMED → PROCESSING (bắt đầu đóng gói) → SHIPPING (bàn giao) → DELIVERED (ghi nhận thủ công). Không bỏ bước, quay lùi, mở lại đơn đã kết thúc hoặc tự xử lý RETURNED qua seller.
- VNPAY chưa PAID có thể xác nhận đơn, nhưng không đóng gói/giao. IPN thành công nay cập nhật tiền/ledger/outbox, giữ nguyên trạng thái seller; không tự đặt PROCESSING. Dữ liệu đơn cũ giữ nguyên trạng thái, không hồi tố.
- Seller chỉ hủy ở PENDING/CONFIRMED/PROCESSING khi UNPAID/FAILED hoặc phiên PENDING đã hết hạn rõ ràng. Không hủy PAID/REFUNDED hoặc đang có payment intent hiệu lực; không hủy sau bàn giao. Hoàn kho và xóa cache sau commit dùng lifecycle hiện có.
- IPN muộn của đơn đã đóng vẫn phải ghi nhận tiền thực tế để đối soát, không bỏ qua thanh toán hoặc tự mở lại đơn. Seller thấy cảnh báo cần hoàn tiền riêng; không phát sự kiện xử lý đơn đang mở cho đơn này. Chưa triển khai refund.
- DELIVERED không đồng nghĩa COD đã thu tiền; paymentStatus giữ nguyên. Action note giải thích việc chưa đối soát, không tạo tracking/vận đơn giả.

SellerOrderPolicy được dùng chung ở service seller và ResourceAccess của route đổi status cũ, để route cũ không bypass điều kiện bước/thanh toán/tài khoản/shop. Ghi seller khóa order với PESSIMISTIC_WRITE, READ_COMMITTED, kiểm tra expectedStatus sau khóa. Nếu target đã là trạng thái hiện tại thì trả thành công idempotent, không hoàn kho lần nữa; nếu đơn đã qua bước khác thì 409 và cần đọc lại. IPN và hủy dùng cùng khóa order, kết quả tiền về sau đóng vẫn được ghi nhận đúng để đối soát.

## Kiểm chứng

- Backend `clean test`: **184 bài liệt kê, 183 pass, 1 schema MySQL dev skip, 0 failure/error**. 15 integration tests mới bao gồm snapshot/quyền/validation/lọc, tuần tự trạng thái, expectedStatus, trùng yêu cầu, hoàn kho/cache, payment gate, REFUNDED, old route và cuộc đua IPN/hủy. Callback thử dùng số giao dịch hợp lệ và assert IPN thực sự thành công.
- Frontend **45/45 tests** và production build pass; năm service tests mới kiểm tra route/bộ lọc/whitelist/danh sách thao tác từ server, compare-and-set, lỗi và chuyển phiên.
- Browser E2E **18/18 pass** trên Chrome và Docker/MySQL thật, gồm bốn ca mới: COD toàn luồng và trạng thái phía khách; VNPAY/active payment/stale tab; lỗi tải/quyền/hủy lặp; tranh chấp trạng thái/IPN. Các luồng OTP/session/sản phẩm/checkout/email/upload trước đó vẫn pass.
- IDE vẫn **No Problems / Java Ready** sau clean, không dùng reload/rebuild/clean cache. Output m2e tiếp tục ở `.ide-build`.

## Giới hạn

Chưa kết nối hãng vận chuyển/tạo vận đơn/tracking, đối soát COD, hủy/đổi trả đơn đã thanh toán hoặc hoàn tiền, tác vụ hàng loạt và dashboard admin. Không đổi theme. Dừng sau chức năng này và PR; phần tiếp theo chờ người dùng yêu cầu.
