## Google Sign-In — 19/11/2026

Triển khai `feat/google-sign-in`: Google là cách chính, xác minh Gmail qua token Google, bỏ OTP riêng cho luồng Google. Người mới xác minh xong chọn buyer/seller; người cũ xác nhận mật khẩu một lần để liên kết và giữ dữ liệu/quyền. Không tạo hoặc tự nâng admin. Giữ một cụm Docker và form mật khẩu dự phòng. Client ID chính chủ đang chờ người dùng tạo, đã có `.env.google.local` và hướng dẫn. Local đã qua 224 backend tests, 55 frontend tests/build và 11 Chrome cases Google/seller/admin; PR sẽ chạy đầy đủ CI trước merge. Google thật vẫn chờ Client ID chính chủ. [Bàn giao](sprints/google-sign-in.md).

## Một cụm Docker chính — 19/11/2026

Thống nhất `docker-compose.yml` project `webbnhngonline`: frontend localhost, API 8080, npm dev 5173 cùng backend. Start-Local giữ volume/dữ liệu cũ, hostname RabbitMQ và uploads, dừng integration nhưng không xóa volume/seed. SMTP thật từ `.env.gmail.local` do người dùng điền. Đã sao lưu hai database; 5 service healthy, dữ liệu cũ nguyên vẹn, API 200 qua Docker/Vite, admin đọc 7 tài khoản thật. Backend 202 tests, frontend 50 tests/build và các kiểm tra trình duyệt đã pass. PR #18 admin đã merge với 22 browser E2E pass CI. SMTP thật chờ chủ dự án điền cấu hình. [Bàn giao](sprints/single-docker-runtime.md).

## Admin quản lý tài khoản — 19/11/2026

PR #17 Gmail OTP đã merge, CI 3/3 gồm 20 browser tests. Tiếp tục `feat/admin-account-management`: màn `/admin/users`, tìm/lọc/phân trang, khóa/mở khóa buyer/seller; API kiểm tra admin thực trong DB, giữ nguyên shop/đơn. Thêm auth_version/JWT để vô hiệu phiên cũ ngay khi khóa và không hồi sinh sau mở khóa. Không tạo/xóa admin hay đổi role qua API công khai. [Bàn giao](sprints/admin-account-management.md).

## Gmail OTP, admin và một môi trường Docker — 19/11/2026

Theo yêu cầu: Gmail + mật khẩu; đăng ký bắt buộc OTP gửi Gmail; số điện thoại chỉ dùng trong địa chỉ nhận hàng. Triển khai lần lượt theo nhánh/PR: (1) `feat/gmail-registration-otp`, (2) quản lý tài khoản buyer/seller cho admin, (3) thống nhất một cụm Docker chính dùng volume dữ liệu cũ. Giữ dữ liệu integration, chỉ dùng khi kiểm thử. Chạy kiểm thử, cập nhật tài liệu, push và merge từng phần vào main.

Phần 1: đã thêm API/form OTP, giới hạn gửi/nhập sai, tiêu thụ nguyên tử; bỏ đăng nhập và cập nhật phone ở hồ sơ. Cấu hình gửi Gmail riêng trong `.env.gmail.local`, người dùng sẽ điền; không khẳng định gửi Gmail thật khi chưa có cấu hình. [Chi tiết](sprints/gmail-registration-otp.md).

# BỘ NHỚ TRẠNG THÁI HỆ THỐNG: HELISHOP CORE

## Sửa kết nối frontend 5173 — 19/11/2026

- Nhánh `fix/frontend-dev-backend-proxy`, bàn giao PR → main. Nguyên nhân đã tái hiện: HTML frontend 5173 trả 200 nhưng API trả 500; Vite hardcode backend localhost:8080 trong khi chỉ backend integration 127.0.0.1:18080 đang chạy. API qua Nginx 18000 vẫn hoạt động.
- `vite.config.ts` đọc `DEV_API_PROXY_TARGET` bằng loadEnv, mặc định 127.0.0.1:8080. Máy này có frontend/.env.development.local trỏ 18080, gitignored; template .example được commit. Giữ API browser /api/v1, proxy cả /api và /uploads, strictPort 5173. Khi đổi backend phải đổi override local.
- Proxy giữ Host giống Nginx (changeOrigin false), để POST cùng origin của frontend không bị Spring đánh giá là cross-origin tới cổng backend. Không sửa/widen CORS backend hoặc trỏ browser trực tiếp tới 18080. Docker/prod vẫn dùng Nginx nội bộ.
- Kiểm chứng: 46 frontend tests + production build pass; test Vite thật với backend tạm kiểm tra API, POST Origin/Host/body và bytes ảnh. Chrome tại localhost:5173 đăng ký 201, đăng nhập/catalog/profile 200, 0 failed API; chỉ tạo tài khoản example.test riêng trên integration. Backend 183 pass + 1 schema MySQL dev skip. [Bàn giao](sprints/frontend-dev-backend-proxy.md).

## Seller xử lý đơn — 19/11/2026

- Nhánh `feat/seller-order-fulfillment`, bàn giao PR → main. Trang `/seller/orders` nối shop/header/sản phẩm; tìm mã, lọc trạng thái/thanh toán, phân trang và chi tiết snapshot hàng/giá/người nhận/ghi chú. API GET list/detail và PATCH status dưới `/api/v1/seller/orders`, user/shop ACTIVE và owner từ JWT/database; chặn chéo shop/caller CUSTOMER/ADMIN/token role cũ.
- SellerOrderPolicy dùng chung route mới và ResourceAccess route status cũ: PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED. Không bỏ bước/quay lùi, không tự RETURNED. VNPAY chỉ đóng gói/giao sau PAID; hủy trước SHIPPING chỉ khi UNPAID/FAILED hoặc PENDING đã hết hạn, không PAID/REFUNDED/intent còn hiệu lực. COD đã giao không tự đổi thành PAID.
- Ghi khóa order, READ_COMMITTED, kiểm tra expectedStatus; trùng target hiện tại idempotent, target khác với trạng thái đã đổi trả 409. Hoàn kho/cache qua lifecycle sau commit chỉ một lần. Frontend nhận allowedStatuses/actionNote từ server, giữ trạng thái thật khi lỗi, có tải lại và chặn request/response cũ khi đổi phiên.
- PaymentService IPN nay giữ nguyên bước seller, chỉ ghi tiền/ledger/outbox. Đơn cũ không hồi tố trạng thái. Tiền về muộn sau hủy/trả vẫn ghi PAID để đối soát, không mở lại đơn hoặc phát sự kiện xử lý đơn đang mở; seller có cảnh báo cần hoàn tiền riêng. Không tuyên bố đã refund.
- Kiểm chứng **184 bài backend liệt kê: 183 pass, 1 MySQL schema dev skip; 45 frontend tests/build; 18 browser E2E pass**. 15 integration tests mới và bốn browser cases bao gồm hủy lặp, stale tab, phân quyền, COD/VNPAY, cache và MySQL IPN/hủy cạnh tranh. Callback thử dùng transaction number hợp lệ và assert RspCode 00 để tránh test thanh toán giả thành công.
- IDE No Problems/Ready sau clean, không reload/rebuild/clean cache. Giữ output .ide-build. Chưa có hãng vận chuyển/tracking/vận đơn, đối soát COD, hủy/trả đã thanh toán hoặc refund/dashboard; giữ theme và dừng sau chức năng này. [Bàn giao](sprints/seller-order-fulfillment.md).

## Tách build Java để tránh Problems tái diễn — 16/11/2026

- Nhánh `fix/java-build-output-isolation`, bàn giao PR → main. POM profile `ide-build` tự kích hoạt theo `m2e.version`: VS Code/m2e ghi `.ide-build`, Maven CLI/CI/Docker vẫn `target`. Cấu hình riêng cả main/test class, resource và generated sources; `.ide-build` bỏ qua Git/Docker. Không dùng setting outputPath dành cho project không Maven.
- Giữ autobuild/Lombok/automatic configuration; bỏ ignore incomplete classpath và setting autoUpdate sai scope ở workspace. Trên máy hiện tại chuyển Red Hat từ pre-release 1.59.2026111308 sang Release 1.58.0, không pin, tự kiểm tra/cập nhật bật ở User Settings; log startup mới không còn bundle ASM collision. Không ghi version cố định vào repo.
- Kiểm chứng thực tế: `clean test` 168 pass + 1 schema MySQL dev skip khi IDE vẫn mở; Problems giữ 0, Java Ready, không cần rebuild sau clean. Hash class/mapper IDE không đổi. Package/file tạm sinh đúng 1 lỗi cố ý và tự hết sau sửa; clean test-compile có file mới vẫn pass, đã xóa probe. Khác lần trước, đây là tách output và kiểm chứng hai compiler không còn chia sẻ thư mục.
- [Cấu hình, bằng chứng và hướng khôi phục](java-ide-troubleshooting.md). Các ghi nhận clean-cache bên dưới là lịch sử; không yêu cầu dọn cache theo mỗi lần thêm file.

## Sản phẩm/SKU seller — 16/11/2026

- Nhánh `feat/seller-product-sku-management`, bàn giao PR → main. Trang `/seller/products` nối từ shop/header seller; tìm/lọc/phân trang sản phẩm của chính shop, tạo sản phẩm kèm SKU đầu tiên, sửa metadata/trạng thái và thêm/sửa SKU, reload giữ dữ liệu thật. Không mở rộng sang đơn bán/admin/theme.
- API GET/POST `/api/v1/seller/products`, GET/PUT `/{id}`, POST `/{id}/skus`, PUT `/{id}/skus/{skuId}`: owner/shop lấy từ principal, kiểm tra DB user SELLER ACTIVE và shop ACTIVE. Create nhận `{product, skus}` và lưu nguyên tử; DTO không nhận shopId/rating/aggregate. Mã SKU duy nhất, giá >0, giá gốc ≥giá bán, kho 0–1.000.000, tối đa 100 SKU và hai tên thuộc tính nhất quán, tổ hợp khác nhau. Slug UUID tự sinh.
- ProductWriteLock khóa SKU theo ID tăng dần trước product, READ_COMMITTED cho seller writes; version bắt buộc khi sửa SKU, stale trả 409. Không đổi dữ liệu thì version có thể không tăng. Giá sản phẩm min(SKU.price), tổng kho sum(SKU.stock); cache products xóa sau commit qua ProductStockChanged. Đơn đã mua giữ snapshot giá/biến thể.
- API products cũ không sửa price/stock của sản phẩm có SKU, không tự đặt rating và không mở bán sản phẩm chưa có SKU. ResourceAccess kiểm tra seller/shop ACTIVE cả route cũ. Legacy checkout khóa cùng thứ tự SKU → product và chặn sản phẩm/shop ngừng bán.
- Kiểm chứng: backend 169 bài liệt kê, 168 pass + 1 schema MySQL dev skip; frontend 40 tests/build và 14/14 browser E2E pass. Ba E2E mới trên MySQL/Redis/RabbitMQ/Nginx thật, gồm khách mua biến thể vừa tạo và concurrent SKU edits/checkout.
- IDE sau clean từng giữ 204 import errors dù compiler pass. Đã Clean Java Language Server Workspace → Reload and delete, Java Ready/No Problems. Probe package/file mới báo đúng 1 lỗi cố ý, sửa tự về 0 rồi xóa file thử. Sau external clean phải kiểm chứng IDE; không chỉ dựa vào Maven pass. Không tắt kiểm tra lỗi để che Problems.
- Giới hạn: ảnh dùng URL, chưa upload; không xóa cứng SKU vì đơn cũ tham chiếu, dùng trạng thái sản phẩm để ẩn. Chưa có sửa hàng loạt thuộc tính, import file, dashboard hoặc đơn bán. Chi tiết [bàn giao](sprints/seller-product-sku-management.md).

## Mở gian hàng — 14/11/2026

- Phạm vi đã triển khai: GET/POST `/api/v1/shops/me`, `ShopService`, DTO ApiResponse/OpenAPI và trang `/seller/shop`. Header Kênh Người Bán đưa đến đăng ký seller; đăng ký/đăng nhập SELLER mặc định vào shop, vẫn giữ redirect hợp lệ khi có.
- Một SELLER đang ACTIVE có một shop. Server lấy owner từ principal JWT, kiểm tra role/status database (không tin claim cũ), khóa user khi tạo; unique `owner_id` và `shop_name` ngăn race. Tên trim, 3–150 ký tự, trùng không phân biệt hoa thường trả 409; mô tả tùy chọn ≤2000 ký tự. Shop mới ACTIVE, chưa có admin duyệt; CUSTOMER không tự nâng role.
- GET khi chưa có shop trả ApiResponse 200 với data rỗng (NON_NULL bỏ trường data). Frontend quy về null và chỉ hiện form sau khi GET thành công; lỗi tải có nút thử lại, lỗi lưu giữ form, xung đột do tab khác tạo sẽ đọc shop thật. Không cho tạo thêm khi shop INACTIVE/BANNED. Bỏ kết quả request cũ khi đổi phiên.
- Kiểm chứng: backend clean test 157 pass, 1 test MySQL dev skip, không failure/error; frontend 35 tests và TypeScript/build pass; toàn bộ 11/11 browser E2E pass. Bốn E2E mới kiểm chứng mở shop thật, reload/relogin, trùng tên/quyền, concurrency MySQL và lỗi tải/phục hồi tab khác.
- Bàn giao `feat/seller-shop-onboarding` → PR → main. Dừng tại chức năng này theo yêu cầu; quản lý sản phẩm/đơn bán/dashboard/theme là phần sau. Hướng dẫn và bằng chứng: [seller-shop-onboarding.md](sprints/seller-shop-onboarding.md).

## Bảo trì IDE — 14/11/2026

Sửa gốc lỗi Problems đỏ hàng loạt: bỏ lifecycle mapping m2e `<ignore />` cho `maven-compiler-plugin` trong pom. Mapping đó làm import mới mất Java nature/builder và classpath, dù Maven CLI vẫn build được. Bật m2e JDT APT và bỏ build-helper khai báo generated-sources trùng compiler để MapStruct được IDE quản lý đúng sau clean. Đã dựng lại Java model của VS Code, xác nhận No Problems với JavaSE-21 và classpath Maven thật; dọn 4 import test không dùng, tắt Oracle Java riêng workspace trên máy hiện tại. Backend clean test: 151 pass, 1 test MySQL skip, 0 failure/error. Chi tiết: [Java IDE troubleshooting](java-ide-troubleshooting.md). Bàn giao theo nhánh `fix/java-ide-project-import` → PR → main.

## 1. Trạng thái hiện hành — 13/11/2026

Người dùng yêu cầu **mỗi chức năng tạo nhánh → PR → merge main**, thay cho push thẳng main trong quy tắc cũ. Các chức năng 01–09 đã merge PR #1–#9. Chức năng 10 bàn giao tại [PR #10](https://github.com/vass05/ShopBanHangOnline/pull/10), nhánh `test/browser-checkout-and-ci`; hoàn tất browser E2E và CI trước khi bàn giao PR. Không coi các ghi nhận baseline/Sprint phía dưới là nghiệm thu production.

**Kiểm chứng:** backend 152/152 tests, frontend 31/31 tests và build pass; 7/7 Playwright E2E qua Chrome headless trên Docker thật. Cụm thử `helishop-integration`, container/volume `helishop_integration_*`, web 18000, API 18080, Mailpit 18025. Dev `eshop_*` vẫn chạy; không xóa/chuyển volume khi đổi môi trường.

- JWT principal đã xác minh và ResourceAccess kiểm quyền tài nguyên; không tin customerId trong request, không cho tự đăng ký admin.
- Phiên duy nhất `helishop-session-v3`, rememberMe chọn localStorage/sessionStorage, Axios refresh đồng bộ và chặn response sai tài khoản. OTP BCrypt trong Redis, cooldown/giới hạn thử, gửi qua SMTP; reset thu hồi refresh.
- Giỏ tài khoản lấy Redis HASH `cart:user:{id}` TTL 30 ngày, nhóm shop. Giỏ khách khóa riêng, merge bằng max(quantity) và bỏ response cũ khi đổi phiên.
- Quote/checkout batch tính giá, kho, phí, voucher và địa chỉ server; khóa customer/voucher/SKU/product theo thứ tự, receipt chống trùng. Mỗi shop một đơn trong cùng giao dịch; trừ lượng mua khỏi Redis sau commit.
- Đơn mua đọc API; hủy theo state/owner, khóa vòng đời và hoàn kho một lần. Cache stock xóa sau commit. Không giả tạo ID, PAID, giá giảm/metrics/SKU thiếu từ API.
- VNPAY Return chỉ xác minh; IPN mới ghi thanh toán, kiểm checksum/merchant/amount/status, khóa đơn và ledger/outbox chống trùng. `order_paid_outbox` retry với publisher confirms; RabbitMQ email gửi theo at least once. DLQ giữ thư lỗi, listener chẩn đoán không tự chạy. SMTP có timeout kết nối 3 giây, đọc/ghi 5 giây.
- Frontend dùng `/api/v1` cùng origin; Nginx proxy cả API/uploads với Docker DNS refresh. Ảnh avatar 5 MB JPEG/PNG/WEBP, tên UUID và kiểm tra signature; volume giữ ảnh qua tạo lại backend, UID 10001, múi giờ +07.
- Production tạo file riêng bằng `scripts/Initialize-Environment.ps1`; mật khẩu/JWT ngẫu nhiên và không commit. SMTP/Cloudinary cấu hình ngoài image, VNPAY mặc định khóa. `VITE_API_BASE_URL` là build arg. Hướng dẫn chạy: README gốc.
- Browser E2E dùng fixture không chứa email cá nhân, SMTP Mailpit không relay và secret thanh toán thử; kiểm chứng COD, OTP/refresh, IPN đồng thời/outbox/email, avatar qua recreate và SMTP lỗi vào DLQ. CI ba job chạy PR/main, lưu báo cáo 7 ngày.

**Giới hạn còn lại:** chưa có nghiệm thu merchant/SMTP/Cloudinary thật, domain/TLS, backup/restore hay migration thay ddl-auto update. Seller-notify queue hiện giữ sự kiện; dashboard seller/admin, review/chat/vận chuyển ngoài và payment-group là đợt sau. Không gọi email là exactly once.

Tham chiếu: [kế hoạch và tiến độ](../implementation_plan.md), [bàn giao Docker](sprints/integration-09-docker-uploads.md), [bàn giao E2E/CI](sprints/integration-10-browser-ci.md). Phần thông số/Sprint dưới đây là ghi nhận lịch sử; cấu hình hiện hành đọc từ Compose và file env riêng, không dùng mật khẩu mẫu làm production.

---
## 💻 2. CÔNG NGHỆ VÀ PHIÊN BẢN (TECH STACK MATRIX)

| Thành phần | Công nghệ / Thư viện | Phiên bản | Ghi chú cấu hình |
| :--- | :--- | :---: | :--- |
| **Backend Java** | OpenJDK / Temurin | 21 | Pattern Matching, Record, Virtual Threads ready |
| **Backend Framework** | Spring Boot | 3.3.5 | Spring Security 6, Spring Data JPA, Spring AMQP |
| **Frontend UI** | React | 18.3.1 | Vite 5.4.9, TypeScript 5.6.3 |
| **Styling** | Tailwind CSS | 3.4.14 | Shopee Orange Theme `#EE4D2D`, Glassmorphism, Inter Font |
| **State Management** | Zustand | 5.0.0 | Persist middleware, LocalStorage sync |
| **Server State / Cache** | TanStack Query | 5.59.16 | StaleTime 5m, GC 15m |
| **HTTP Client** | Axios | 1.7.7 | Request/Response Interceptor với hàng đợi `failedQueue` |
| **Icons** | Lucide React | 0.453.0 | Icon pack hiện đại |
| **Cơ sở dữ liệu** | MySQL | 8.0 | Port 3307 (Host) -> 3306 (Container), utf8mb4 |
| **Bộ nhớ đệm / Giỏ hàng** | Redis | 7-alpine | Port 6379, AOF Persistent (`appendonly yes`) |
| **Message Broker** | RabbitMQ | 3.13-management | Port 5672 (AMQP), 15672 (Management UI) |
| **Tài liệu API** | Springdoc OpenAPI (Swagger 3) | 2.6.0 | UI: `http://localhost:8080/swagger-ui/index.html` |
| **Xác thực** | JJWT (io.jsonwebtoken) | 0.12.6 | Access Token (15 phút), Refresh Token (7 ngày) |

---

## 🌐 3. BẢNG THÔNG SỐ CỔNG & TÀI KHOẢN MẶC ĐỊNH

### 3.1. Cổng Dịch Vụ
- **Application Backend**: `http://localhost:8080`
- **Frontend Web Application**: `http://localhost:5173` (React 18 + Vite)
- **Swagger Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **MySQL Database**: `localhost:3307` (user: `eshop_user`, pass: `eshop_secret`, db: `eshop_db`)
- **Redis Server**: `localhost:6379`
- **RabbitMQ Dashboard**: `http://localhost:15672` (user: `guest`, pass: `guest`)
- **RabbitMQ AMQP**: `localhost:5672`

### 3.2. Cặp Khóa Bí Mật & TTL
- **JWT Secret**: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
- **Access Token TTL**: 15 phút (`900,000` ms)
- **Refresh Token TTL**: 7 ngày (`604,800` giây)
- **Redis Cart Hash TTL**: 30 ngày (`2,592,000` giây)

---

## ⚠️ 4. BẪY KỸ THUẬT & GIẢI PHÁP ĐÃ ĐƯỢC XÁC LẬP (PITFALLS & WORKAROUNDS)

### 1. Lỗi xung đột tên ApiResponse trong Controller
- **Vấn đề**: `io.swagger.v3.oas.annotations.responses.ApiResponse` bị trùng tên với `com.helishop.core.common.response.ApiResponse`.
- **Giải pháp bắt buộc**: Không import Swagger ApiResponse. Luôn chú thích dạng đầy đủ: `@io.swagger.v3.oas.annotations.responses.ApiResponse(...)`.

### 2. Xung đột Annotation Processor (Lombok 1.18.34 & MapStruct)
- **Vấn đề**: Khi compile Maven trên Java 21, Lombok mặc định của Spring Boot parent có thể gây lỗi nạp MapStruct.
- **Giải pháp**: Định nghĩa rõ ràng thuộc tính `<lombok.version>1.18.34</lombok.version>` trong `pom.xml`.

### 3. Lỗi PowerShell Command Chaining
- **Vấn đề**: PowerShell không chấp nhận toán tử `&&` để nối lệnh trong các phiên bản cũ hoặc cấu hình mặc định (VD: `git add . && git commit`).
- **Giải pháp**: Luôn dùng dấu chấm phẩy `;` để phân tách lệnh: `git add . ; git commit -m "..."`.

### 4. Lỗi PowerShell đối số có dấu phẩy
- **Vấn đề**: Lệnh như `mvn test -Dtest=TestA,TestB` bị PowerShell hiểu nhầm dấu phẩy là phần tử mảng.
- **Giải pháp**: Luôn bọc đối số trong ngoặc kép: `mvn test "-Dtest=TestA,TestB"`.

### 5. Kiểm thử Mockito Strictness với RedisTemplate
- **Vấn đề**: Mockito 5 mặc định strict stubbing. Nếu stub `stringRedisTemplate.opsForHash()` trong `@BeforeEach` nhưng test case gọi `clearCart()` (chỉ gọi `delete()`), Mockito sẽ ném lỗi `UnnecessaryStubbingException`.
- **Giải pháp**: Sử dụng `lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);`.

### 6. Định dạng phản hồi VNPAY IPN Webhook Server-to-Server
- **Vấn đề**: VNPAY IPN yêu cầu định dạng JSON chính xác `{"RspCode":"00","Message":"Confirm Success"}` với trường viết hoa `RspCode` và `Message`. Nếu bọc trong `ApiResponse<T>` chuẩn của hệ thống, máy chủ VNPAY sẽ báo lỗi không nhận dạng được và liên tục gọi lại.
- **Giải pháp**: Tạo DTO `VnPayIpnResponse` với `@JsonProperty("RspCode")` và `@JsonProperty("Message")`, trả về trực tiếp từ controller `/api/v1/payments/vnpay-ipn`.

### 7. Spring Boot MailSenderAutoConfiguration trong môi trường Test
- **Vấn đề**: `MailSenderAutoConfiguration` chỉ tạo bean `JavaMailSender` khi thuộc tính `spring.mail.host` tồn tại. Nếu `src/test/resources/application.yml` thiếu `spring.mail.host`, toàn bộ `@SpringBootTest` quét qua `EmailService` sẽ gặp lỗi `UnsatisfiedDependencyException`.
- **Giải pháp**: Định nghĩa `spring.mail.host: localhost` trong `src/test/resources/application.yml`.

### 8. Biến @Value trong Mockito Unit Test thuần túy
- **Vấn đề**: Các test case chạy bằng `@ExtendWith(MockitoExtension.class)` không khởi tạo Spring Context, dẫn đến các trường có `@Value` bị null nếu không được gán qua reflection.
- **Giải pháp**: Luôn gán giá trị mặc định trực tiếp lúc khai báo trường trong class (ví dụ: `private String senderEmail = "helishop.system@gmail.com";`).

### 9. Cấu hình CORS Backend & Vite Proxy
- **Vấn đề**: Khi chạy frontend Vite trên `http://localhost:5173`, nếu Spring Security chặn CORS với `AbstractHttpConfigurer::disable` thì trình duyệt sẽ chặn toàn bộ các request preflight `OPTIONS`.
- **Giải pháp**: Cấu hình `CorsConfigurationSource` cho phép `allowedOriginPatterns("*")`, methods, headers `*` và `allowCredentials(true)` trong `SecurityConfig.java`, đồng thời thiết lập Vite proxy forwarding `/api` trực tiếp về `http://localhost:8080`.

### 10. Chống Race Condition Refresh Token với failedQueue
- **Vấn đề**: Khi Access Token hết hạn, nếu trang web cùng lúc bắn ra 5-10 request song song (ví dụ: lấy thông tin user, giỏ hàng, thông báo, danh mục), tất cả các request này đều nhận mã 401. Nếu không có cơ chế hàng đợi, cả 10 request sẽ đồng thời gọi `POST /auth/refresh-token`, dẫn tới việc Refresh Token bị thu hồi hoặc vi phạm tính toàn vẹn phiên làm việc.
### 11. Cơ chế Nạp dữ liệu mẫu bằng SQL (Manual Seed Script) & Quy chuẩn Tài khoản
- **Đặc điểm**: Hệ thống chuyển hẳn sang sử dụng tệp SQL chi tiết [backend/src/main/resources/seed-data.sql](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/backend/src/main/resources/seed-data.sql) chuẩn MySQL 8.0, loại bỏ hoàn toàn `DatabaseSeeder.java` để người dùng toàn quyền kiểm soát thời điểm và dữ liệu nạp vào DB.
- **Ràng buộc đăng ký**:
  1. Mỗi Gmail chỉ được đăng ký duy nhất 1 tài khoản (ràng buộc `UNIQUE` trên cột `email` và kiểm tra `userRepository.existsByEmail` tại `AuthService.register`).
  2. Mỗi số điện thoại chỉ liên kết với 1 tài khoản duy nhất (`userRepository.existsByPhone`).
  3. Hỗ trợ đăng nhập linh hoạt bằng cả Gmail hoặc Số điện thoại (`findByEmail(id).or(() -> findByPhone(id))`).

### 12. Tài khoản kiểm thử chuẩn xác hệ thống
- **Khách hàng chính (CUSTOMER)**:
  - **Họ và tên**: `Vũ Viết Anh`
  - **Gmail**: `vuvietanh@gmail.com`
  - **Số điện thoại**: `0988889999`
  - **Mật khẩu**: `Password123!`
  - **Địa chỉ giao hàng mặc định**: `Thôn Tốt Động, Xã Quảng Bị, Huyện Chương Mỹ, TP. Hà Nội`
- **Quản trị viên sàn (ADMIN)**: `admin.helishop@gmail.com` (hoặc `0901111111`) / `Password123!`
- **Người bán hàng Shopee Mall (SELLER)**:
  - Apple Store: `seller.apple@gmail.com` (hoặc `0902222222`) / `Password123!`
  - Sony Official: `seller.sony@gmail.com` (hoặc `0903333333`) / `Password123!`
  - NuPhy Studio: `seller.nuphy@gmail.com` (hoặc `0904444444`) / `Password123!`
  - Coolmate Store: `seller.coolmate@gmail.com` (hoặc `0905555555`) / `Password123!`
  - Logitech G Store: `seller.logitech@gmail.com` (hoặc `0906666666`) / `Password123!`
  - Anker Store: `seller.anker@gmail.com` (hoặc `0907777777`) / `Password123!`
- **Vouchers có sẵn**: `HELI50K` (giảm 50.000₫), `HELI100K` (giảm 100.000₫), `HELI500K` (giảm 500.000₫), `VNPAY10` (giảm 10%), `FREESHIP` (miễn phí vận chuyển 30.000₫).

---

## 📋 5. BÀN GIAO & VẬN HÀNH HỆ THỐNG
- **Dữ liệu thực tế (rà soát 13/11/2026)**: Catalog, auth và hồ sơ có gọi API; xóa tệp mockData không đồng nghĩa toàn bộ frontend đã dùng dữ liệu backend. Giỏ hàng và lịch sử đơn hiện dùng Zustand persist; checkout tự tạo đơn cục bộ và chưa gọi `/orders/checkout`.
- **Giao diện khi Backend chưa bật / chưa nạp data**: Hiển thị bảng thông báo trạng thái kết nối máy chủ rõ ràng kèm hướng dẫn khởi động Spring Boot và nạp `seed-data.sql`, tích hợp nút "Thử kết nối lại" (Retry).
- **Trạng thái giỏ hàng & Đơn hàng**: Store khởi tạo rỗng nhưng lưu ở trình duyệt, chưa đồng bộ Redis/MySQL hoặc phân tách theo tài khoản; trang hủy đơn chỉ cập nhật store cục bộ.
- **Tình trạng kiểm thử**: Backend 103/103 tests và build frontend đạt ở baseline ngày 13/11/2026. Test backend dùng H2 và có mock dịch vụ ngoài; chưa đủ để khẳng định đã nghiệm thu end-to-end frontend/backend hoặc production Docker.




