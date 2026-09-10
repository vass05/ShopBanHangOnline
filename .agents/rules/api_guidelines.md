# Quy Chuẩn Lập Trình & Thiết Kế API

## 1. Cấu trúc phản hồi (Response Format)
- Mọi API endpoint trả về dữ liệu đều phải bọc trong `ApiResponse<T>`.
- Trạng thái thành công HTTP 200/201 kèm mã code = 200 trong body:
  ```java
  return ApiResponse.success(payload, "Thông điệp mô tả");
  ```

## 2. OpenAPI / Swagger 3.0
- Controller: Đánh dấu bằng `@Tag(name = "...", description = "...")`.
- Endpoint:
  - Gắn `@Operation(summary = "...", description = "...")`.
  - Gắn `@ApiResponses` với `@io.swagger.v3.oas.annotations.responses.ApiResponse` (tránh xung đột import).

## 3. Quản lý ngoại lệ & Mã lỗi
- Ném ngoại lệ nghiệp vụ thông qua `AppException(ErrorCode.<CODE>, message)`.
- Các mã lỗi phổ biến:
  - `RESOURCE_NOT_FOUND` (404)
  - `INSUFFICIENT_STOCK` (409)
  - `UNAUTHENTICATED` (401)
  - `UNAUTHORIZED` (403)
  - `BAD_REQUEST` (400)
