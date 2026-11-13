# Kế hoạch đồng bộ frontend và backend HeliShop

Ngày rà soát: **13/11/2026 (Asia/Saigon)**. Baseline mã nguồn: `acdd254`.

## Tiến độ triển khai theo chức năng

Theo yêu cầu ngày 13/11/2026: mỗi chức năng có nhánh riêng, kiểm thử, PR vào main và merge trước khi bắt đầu chức năng tiếp theo. Quy trình PR này thay cho push trực tiếp main.

| Chức năng | Nhánh | Trạng thái kiểm chứng |
| --- | --- | --- |
| Định danh và quyền sở hữu tài nguyên | `fix/identity-and-resource-ownership` | PR #1 đã merge; backend 109/109 tests pass |
| Phiên đăng nhập và refresh token | `fix/auth-session-persistence` | PR #2 đã merge; frontend 10/10 tests và production build pass |
| OTP khôi phục mật khẩu | `fix/password-reset-otp` | PR #3 đã merge; backend 116/116 tests pass |
| Giỏ hàng Redis | `feat/redis-cart-integration` | PR #4 đã merge; frontend 15/15 tests và production build pass |
| Checkout, voucher/phí và nhiều shop | `feat/server-checkout-and-pricing` | PR #5 đã merge; backend 130/130 tests pass; frontend 18/18 tests và build pass |
| Lịch sử đơn và hủy đơn | `feat/order-history-and-cancellation` | PR #6 đã merge; backend 132/132 tests; frontend 22/22 tests/build pass |
| VNPAY và kết quả thanh toán | `fix/vnpay-return-and-ipn` | PR #7 đã merge; backend 144/144 tests; frontend 26/26 tests/build pass |
| Catalog và phân trang | `feat/server-catalog-pagination` | PR #8 đã merge; backend 149/149 tests; frontend 30/30 tests/build pass |
| Docker và uploads | `fix/docker-runtime-and-uploads` | Backend 152/152 tests; frontend 31/31 tests/build pass; 5/5 Docker healthy, upload giữ nguyên sau tạo lại backend; bàn giao qua PR |
| E2E và CI | Chưa tạo | Chờ triển khai |

## 1. Kết luận và phạm vi kiểm chứng

**Frontend và backend chưa khớp hoàn chỉnh ở luồng mua hàng.** Catalog, xác thực cơ bản và hồ sơ đã có kết nối API; giỏ hàng, đặt hàng và lịch sử đơn vẫn phụ thuộc dữ liệu trong trình duyệt. Chưa đạt điều kiện bàn giao luồng mua hàng tích hợp hoặc triển khai cho người dùng thật.

Các ma trận và bằng chứng rà soát bên dưới ghi nhận baseline `acdd254`. Tiến độ sửa hiện tại nằm ở bảng theo chức năng phía trên; chỉ đánh dấu hoàn thành phần đã kiểm chứng và bàn giao qua PR.

Đã chạy trong lần rà soát này:

- Backend: `cd backend; .\mvnw.cmd test` — **103 tests, 0 failures, 0 errors, 0 skipped**.
- Frontend: `tsc --noEmit` ở thư mục frontend — exit 0; kiểm tra chính bằng `npm run build` (`tsc -b && vite build`) — **thành công, 1707 modules**. `tsconfig.json` gốc dùng project references nên không dùng riêng `--noEmit` để kết luận đã kiểm tra toàn bộ ứng dụng.
- Build frontend lần đầu bị sandbox chặn tiến trình esbuild (`spawn EPERM`); chạy lại ngoài sandbox thành công, không sửa mã nguồn.
- Các test backend dùng cấu hình H2; bài test tên E2E payment/email có mock RabbitTemplate, EmailService và Redis. Kết quả này chưa chứng minh trình duyệt, MySQL, Redis, RabbitMQ, SMTP và cổng VNPAY thật hoạt động cùng nhau.
- Chưa chạy browser E2E, chưa khởi động hoặc kiểm chứng toàn bộ cụm production Docker trong lần rà soát này.

## 2. Ma trận mức độ khớp

| Luồng | Frontend hiện tại | Backend hiện tại | Kết luận / việc cần làm |
| --- | --- | --- | --- |
| Đăng ký / đăng nhập | POST `/auth/register`, `/auth/login`; chuyển `userId` thành `user.id` | Cùng route và trường dữ liệu chính trong AuthResponse | Khớp ở mức hợp đồng; bổ sung kiểm chứng trình duyệt và chặn tự cấp vai trò ADMIN |
| Refresh / logout | Đọc `data.data.accessToken`, hàng đợi request, POST logout | Có refresh-token và logout | Khớp route/payload; interceptor chưa đồng bộ access token mới vào Zustand; persist vẫn ghi token vào localStorage khi bỏ ghi nhớ; cần kiểm thử hết phiên và đăng nhập sai |
| Hồ sơ / mật khẩu | profileService GET/PUT `/users/me`, PUT password | Cùng route và DTO | Khớp ở mức hợp đồng; kiểm chứng lỗi validation và trạng thái phiên |
| Sổ địa chỉ trong hồ sơ | CRUD `/users/me/addresses`, chọn mặc định | Cùng route và tên trường | Khớp ở mức hợp đồng; checkout chưa sử dụng dữ liệu này |
| Avatar | Multipart `file`, URL `/uploads/...` bị nối cố định localhost | Lưu file vào `uploads/avatars`, trả URL tương đối | Local có cấu trúc tương thích; Docker còn thiếu proxy uploads, volume và thư mục ghi được cho appuser; thống nhất giới hạn multipart |
| Danh mục | GET `/categories/tree` | Trả cây danh mục trong ApiResponse | Khớp cơ bản; mapper trang chủ chỉ giữ thêm một cấp children |
| Catalog / chi tiết SKU | GET `/products?size=50`, `/products/{id}`; có mapper `skus → productSkus` và JSON thuộc tính | Trả PageResponse.items, ProductResponse.skus | Khớp phần chính nhờ mapper; vẫn tự tạo lượt bán, lượt đánh giá, giá gốc và thông tin shop; PDP tự tạo SKU khi thiếu dữ liệu |
| Tìm kiếm / lọc / sắp xếp | Xử lý trên tối đa 50 sản phẩm đã tải | Có keyword, categoryId, khoảng giá, minRating, sortBy, page bắt đầu từ 1 | Chưa khai thác đúng API; tìm kiếm không bao phủ toàn catalog |
| Giỏ hàng | Zustand persist `helishop-cart-storage-v2`; thêm/sửa/xóa cục bộ | Redis HASH và API GET/POST/PUT/DELETE `/cart` | Chưa nối API nghiệp vụ. Dashboard chỉ có request thử GET cart |
| Checkout COD | Tạo ID ngẫu nhiên, mã đơn và lưu local | POST `/orders/checkout?customerId=...`, kiểm giá SKU và khóa trừ kho | Chưa nối: không tạo đơn MySQL, không trừ kho backend |
| Đơn mua / hủy / mua lại | Đọc local store, hủy bằng updateOrderStatus cục bộ; mua lại dùng productId=1, stock=50 | GET đơn theo ID/mã/customer; PATCH status | Chưa nối; cần gọi API và xác thực SKU/tồn kho khi mua lại |
| VNPAY | Gửi `{orderId, bankCode}`; lỗi API vẫn chuyển trang success; tự gán PAID | DTO yêu cầu `{orderCode, bankCode?, language?}`; chỉ IPN cập nhật DB | Lệch payload và nguồn trạng thái; phải sửa trước nghiệm thu thanh toán |
| Trang kết quả thanh toán | Tin query `status=success` / response code; không kiểm chứng server; đọc trực tiếp vnp_Amount | Return endpoint trả ApiResponse JSON đã kiểm chữ ký; amount backend đã chia 100 | Chưa có cầu nối return → UI; nếu đọc vnp_Amount thô UI hiển thị gấp 100 lần |
| Địa chỉ checkout | Dùng DEFAULT_ADDRESS, tự chỉnh trên trang | Sổ địa chỉ người dùng đã có API | Chưa nối; chuyển sang chọn địa chỉ của tài khoản hiện hành |
| Voucher / phí vận chuyển | Danh sách mã và phí cố định; tự tính và áp dụng giảm giá | Có entity Voucher nhưng chưa có API áp dụng; checkout đang đặt shippingFee/discountAmount bằng 0 | Chưa khớp tiền thanh toán; cần backend báo giá và kiểm lại khi đặt |
| Nhiều shop | UI nhóm giỏ theo shop, đơn local gán shop đầu tiên | Checkout trả một đơn, chưa gán shop từ request/SKU | Chưa có cơ chế tách đơn thật; cần hợp đồng checkout theo từng shop |
| Seller / Admin | Dashboard chủ yếu hiển thị phiên và thử interceptor | Có tạo/sửa sản phẩm, chưa có API tổng hợp dashboard vận hành | Chưa có màn quản trị bán hàng hoàn chỉnh; làm sau luồng khách hàng |
| Docker frontend → backend | `.env`, `.env.development` và fallback Axios dùng localhost:8080 | Nginx đã proxy `/api/` | Chạy từ máy khác sẽ gọi backend trên máy người xem; dùng `/api/v1` cùng origin và cấu hình ở thời điểm build |

## 3. Bằng chứng trọng yếu

- [Giỏ hàng cục bộ](frontend/src/store/useCartStore.ts), [đơn hàng cục bộ](frontend/src/store/useOrderStore.ts).
- [Checkout](frontend/src/pages/CheckoutPage.tsx): DEFAULT_ADDRESS, AVAILABLE_VOUCHERS, tạo ID ngẫu nhiên, addOrder/removeSelected trước khi gọi thanh toán, gửi orderId và fallback success.
- [OrdersPage](frontend/src/pages/OrdersPage.tsx): đọc store và hủy cục bộ. [PaymentResultPage](frontend/src/pages/PaymentResultPage.tsx): tin query và không chuẩn hóa vnp_Amount.
- [DTO thanh toán](backend/src/main/java/com/helishop/core/modules/payment/dto/CreatePaymentUrlRequest.java), [return controller](backend/src/main/java/com/helishop/core/modules/payment/controller/PaymentController.java), [payment service](backend/src/main/java/com/helishop/core/modules/payment/service/PaymentService.java).
- [CheckoutRequest](backend/src/main/java/com/helishop/core/modules/order/dto/CheckoutRequest.java), [OrderResponse](backend/src/main/java/com/helishop/core/modules/order/dto/OrderResponse.java), [OrderService](backend/src/main/java/com/helishop/core/modules/order/service/OrderService.java).
- [API client](frontend/src/lib/api.ts), [auth store](frontend/src/store/useAuthStore.ts), [Nginx](frontend/nginx.conf), [Docker backend](backend/Dockerfile), [Compose production](docker-compose.production.yml).

## 4. Lỗi cần xử lý trước khi nối luồng mua hàng

1. **Định danh và quyền sở hữu:** SecurityUtils ưu tiên customerId do client cung cấp; OrderController nhận customerId trực tiếp; API đọc/cập nhật đơn chỉ yêu cầu đăng nhập và service chưa kiểm tra chủ sở hữu. Chuyển sang danh tính trong SecurityContext, kiểm tra quyền theo đơn/shop và giới hạn chuyển trạng thái theo vai trò.
2. **Vai trò đăng ký:** RegisterRequest nhận enum gồm ROLE_ADMIN và AuthService dùng trực tiếp. Chỉ cho đăng ký vai trò được phép; việc cấp quyền quản trị phải qua luồng quản trị riêng.
3. **OTP:** forgot-password hiện trả OTP trong thông điệp API và ghi OTP vào log, kể cả khi gửi SMTP thất bại. Bỏ nội dung thử nghiệm ở môi trường vận hành, xử lý lỗi gửi thực tế và giới hạn thử/gửi mã.
4. **Số lượng và vòng đời đơn:** CheckoutRequest.ItemRequest chỉ có NotNull, chưa có ràng buộc số lượng dương; service chưa kiểm tra quantity > 0 trước phép trừ kho. Thêm validation và kiểm tra nghiệp vụ; ràng buộc chuyển trạng thái, chống hoàn kho lặp khi gọi đồng thời.
5. **Thanh toán:** Không đánh dấu PAID từ browser. Xóa fallback success, dùng trạng thái xác nhận từ backend và IPN. Bổ sung kiểm chứng IPN trùng/đồng thời, số tiền sai, đơn đã hủy và sự kiện email chỉ phát một lần theo nghiệp vụ.

## 5. Hợp đồng dữ liệu mục tiêu

Giữ `ApiResponse<T>` cho API ứng dụng. IPN giữ nguyên định dạng riêng của VNPAY, không bọc thêm ApiResponse.

| Thành phần | Chênh lệch hiện tại | Quyết định triển khai |
| --- | --- | --- |
| Product | Backend `skus`, `skuAttributes` là chuỗi JSON; UI `productSkus`, `attributes` | Tách DTO API và view model, dùng một mapper chung cho trang chủ/PDP; không tự tạo SKU mua được |
| Cart | Backend `shops`, `productTitle`, `skuVariant`, `stockAvailable`; UI `shopGroups`, `productName`, `attributes`, `stockQuantity` | Dùng DTO backend làm hợp đồng và mapper có kiểu; badge lấy totalQuantity, không nhầm totalItems là số lượng |
| Order | Backend `orderStatus`, `finalAmount`, `productNameSnapshot`, `priceAtPurchase`, `subtotal`; UI `status`, `totalAmount`, `productName`, `unitPrice`, `totalPrice` | Phân biệt tiền hàng và tiền cuối; mapper rõ ràng; bổ sung shopName và thông tin hiển thị còn thiếu nếu cần |
| PaymentStatus | Backend có UNPAID, UI union thiếu UNPAID | Bổ sung đầy đủ trạng thái và nhãn UI; tách trạng thái giao hàng khỏi trạng thái trả tiền |
| Address | Hồ sơ dùng recipientName/provinceCity/streetAddress; checkout dùng receiverName/province/detailAddress | Dùng một DTO API chung và snapshot địa chỉ tại thời điểm đặt |
| Pagination | Backend bắt đầu page=1, Dashboard có request thử page=0 | Thống nhất page=1; tiêu thụ items/totalPages/totalElements và lọc ở server |

Thiết kế **đề xuất**, chưa có trong backend hiện tại:

- Endpoint báo giá checkout nhận SKU/số lượng, địa chỉ, phương thức giao hàng và voucher; server trả tiền theo từng shop và tổng cuối.
- Checkout nhiều shop tạo một nhóm đơn trong một giao dịch DB, mỗi đơn gắn đúng shop; khóa SKU theo thứ tự xác định để giảm deadlock. Chốt response danh sách đơn trước khi sửa frontend.
- Giai đoạn đầu thanh toán VNPAY **từng đơn theo shop** qua orderCode; không gửi tổng tiền nhiều đơn vào endpoint vốn chỉ hỗ trợ một đơn. Thanh toán một lần cho cả nhóm cần payment-group riêng, là phần mở rộng.
- Idempotency key cho checkout; backend tính lại giá, voucher, tồn kho và phí khi ghi đơn, không tin tổng tiền client.
- API đơn của tài khoản hiện hành hoặc endpoint cũ đã kiểm quyền; khách chỉ hủy đơn trong trạng thái cho phép, seller chỉ quản lý đơn thuộc shop.
- Trình duyệt tới `/payment-result` qua redirect của backend hoặc frontend gửi query return về endpoint xác minh; UI đọc đơn từ backend, có trạng thái chờ IPN và thử thanh toán lại.

## 6. Kế hoạch triển khai theo thứ tự phụ thuộc

Ước lượng sơ bộ cho một lập trình viên, chưa gồm thời gian chờ tài khoản nhà cung cấp; điều chỉnh sau giai đoạn 1.

| Giai đoạn | Ưu tiên / ước lượng | Công việc | Tiêu chí nghiệm thu |
| --- | --- | --- | --- |
| 1. Chốt hợp đồng và quyền truy cập | P0 / 2–3 ngày | Tách DTO/view model; chuẩn hóa ApiResponse và enum; sửa định danh, ownership, role đăng ký, OTP, quantity và chuyển trạng thái; sửa phiên/refresh | Tài khoản A không đọc/sửa giỏ, đơn, sản phẩm shop của B; không tự đăng ký ADMIN; quantity <= 0 bị chặn; đăng nhập sai không gây vòng refresh; không ghi nhớ không lưu token bền vững |
| 2. Giỏ hàng Redis | P0 / 1–2 ngày, sau GĐ1 | cartService typed; nối thêm/sửa/xóa/xóa nhiều; tải giỏ theo phiên, xử lý giỏ khách khi đăng nhập; đồng bộ badge và lựa chọn, xóa cache khi đổi tài khoản | Reload hoặc đăng nhập thiết bị khác thấy đúng giỏ server; không lẫn tài khoản; hết hàng báo lỗi; TTL được gia hạn 30 ngày, response nhóm đúng shop |
| 3. Checkout COD và đơn mua | P0 / 3–4 ngày, sau GĐ1–2 | Dùng sổ địa chỉ; báo giá server, voucher/phí thật; checkout nhóm đơn theo shop, idempotency; đọc/hủy đơn qua API; bỏ ID/stock tự tạo | Mua 2 shop sinh đúng đơn và tiền; DB lưu đơn, kho trừ đúng; bấm lặp không tạo trùng; lỗi không báo thành công; hủy hợp lệ hoàn kho một lần; giỏ xóa đúng SKU đã đặt thành công |
| 4. Thanh toán VNPAY | P0 / 2–3 ngày, sau GĐ3 | Gửi orderCode thật; cấu hình credential/return/IPN; điều hướng về UI, xác minh và đọc trạng thái server; retry payment cho đơn chưa trả | Thành công/thất bại/hủy/chữ ký sai/sai tiền/IPN chậm hoặc lặp đều hiển thị đúng; VND đúng đơn vị; PAID chỉ theo xác nhận backend; không có fallback success |
| 5. Catalog và triển khai Docker | P1 / 2–3 ngày, sau hợp đồng GĐ1, nghiệm thu sau GĐ4 | Query/filter/pagination server; bỏ số liệu bán hàng tự dựng; `/api/v1` cùng origin; proxy uploads với ưu tiên location đúng trước regex ảnh; volume/quyền ghi avatar; env ngoài mã nguồn; hướng dẫn seed và giữ volume | Tìm thấy sản phẩm ngoài 50 dòng đầu; zero stock không thành hàng có sẵn; frontend truy cập từ máy khác gọi đúng API; avatar tải lại được sau recreate; cụm production healthy |
| 6. Kiểm chứng tích hợp và bàn giao | P0 cho phát hành / 2–3 ngày, sau GĐ1–5 | Browser E2E cho login → SKU → cart → COD/VNPAY → orders/cancel; dùng MySQL/Redis/RabbitMQ thật trong môi trường kiểm thử; kiểm tra SMTP/DLQ; tạo CI build/test; cập nhật README/ROADMAP/MEMORY | Có bằng chứng chạy luồng xuyên suốt; không mất/nhầm giỏ hoặc đơn giữa tài khoản; xác minh stock/payment trong DB và Redis; toàn bộ build/test pass; commit và push origin/main |

Tổng sơ bộ: **12–18 ngày làm việc**. Seller/Admin CRUD, chat, review, thông báo và dashboard kinh doanh là giai đoạn mở rộng sau khi nghiệm thu luồng mua hàng; không dùng màn debug hiện tại để đánh dấu đã xong các chức năng này.

## 7. Điều kiện hạ tầng trước nghiệm thu production

- Kiểm tra Compose project và container đang có trước khi chạy cụm mới: dev/prod cùng dùng các cổng 8080, 3307, 6379, 5672. Chọn cụm và volume dữ liệu rõ ràng; không chạy cả hai với port mapping trùng, không dùng `down -v` để chuyển môi trường.
- Production Compose dùng bộ volume MySQL riêng; không giả định dữ liệu đã seed ở cụm dev tự xuất hiện trong cụm production. Seed có kiểm soát trên môi trường kiểm thử, không ghi đè dữ liệu đang dùng.
- Cấu hình SMTP, Cloudinary nếu sử dụng, VNPAY sandbox hợp lệ và callback công khai cho IPN. Các giá trị mẫu trong application.yml chưa phải cấu hình nhà cung cấp đã được xác minh.
- Frontend Vite lấy biến môi trường khi build; đặt biến ở container Nginx sau khi build không tự thay URL trong bundle. Ưu tiên URL tương đối để dùng chung origin.
- Chuẩn bị persistent storage cho uploads, backup DB và bước kiểm tra health/log sau khởi động; kiểm chứng build image và quyền ghi trên đúng appuser.

## 8. Definition of Done cho đợt đồng bộ

- Mọi thao tác mua hàng chính dùng dữ liệu backend, UI phản ánh loading/error/empty và không tự báo giao dịch thành công.
- Bảo toàn Redis HASH TTL 30 ngày, phân nhóm shop, khóa PESSIMISTIC_WRITE khi đặt hàng và DLQ cho worker quan trọng.
- Có test quyền sở hữu/role, số lượng, tồn kho đồng thời, hủy/hoàn kho, idempotency checkout/payment và lỗi dịch vụ ngoài.
- Build frontend, test backend và browser E2E trên môi trường tích hợp đạt; Swagger và hợp đồng được cập nhật.
- Mỗi giai đoạn có ghi nhận kết quả thực tế, tổng kết sprint trong docs/sprints khi hoàn thành sprint, commit Conventional Commits và push origin/main; không tuyên bố hoàn thành chỉ dựa trên build.
