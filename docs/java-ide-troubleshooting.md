# Java trong VS Code

## Cấu hình dự án

Backend dùng Java 21 và `maven-compiler-plugin` để cấu hình nguồn, classpath và annotation processor. Giữ mapping mặc định của m2e; không khai báo `<ignore />` cho các goal `compile`/`testCompile`. Mapping ignore khiến m2e không thêm Java nature/builder, do đó language server không nhận các package của chính dự án.

Lombok được hỗ trợ bởi Red Hat Java; MapStruct được Maven hoặc JDT APT sinh trong `target/generated-sources/annotations`. Đặt `m2e.apt.activation=jdt_apt` để IDE quản lý annotation processing theo `annotationProcessorPaths`. Không dùng build-helper khai báo lại thư mục sinh mặc định mà compiler đã quản lý. Sau clone hoặc khi DTO/mapper thay đổi, chạy:

```powershell
cd backend
.\mvnw.cmd test-compile
```

Các thư viện, processor và Java release tiếp tục lấy từ `pom.xml`. Không cần viết getter/constructor thủ công chỉ để làm mất lỗi IDE.

## Khôi phục khi Problems giữ lỗi cũ

1. Sử dụng **Language Support for Java by Red Hat** và Java 21. Nếu Oracle Java đang chạy bằng JDK 17 và báo không khởi động được, đặt đúng JDK 21 cho extension đó hoặc chọn **Disable (Workspace)** khi workspace đã dùng Red Hat Java.
2. Chạy **Java: Clean Java Language Server Workspace**, chọn **Reload and delete** để IDE xóa cache và reload.
3. Chạy **Java: Import Java Projects into Workspace**, rồi **Java: Reload Projects** nếu cần. Trong phiên bản extension cũ, lệnh sau có tên **Java: Update Project Configuration**. Sau lần sửa mapping hoặc khi Maven tạo lại toàn bộ file MapStruct, có thể cần clean/reload lại để loại model Java cũ trong bộ nhớ.
4. Đợi Java báo Ready; mở lại một controller và service để xác nhận chẩn đoán. Chỉ thấy Problems bằng 0 chưa đủ: dự án phải được nhận là Java và có classpath Maven.

Việc dọn cache chỉ tác động trạng thái IDE. Không xóa mã nguồn hoặc volume Docker để sửa lỗi này.

## Kiểm chứng sửa lỗi ngày 14/11/2026

Đã xác nhận trực tiếp trong VS Code: ban đầu 346 lỗi và 4 cảnh báo; sau khi bỏ compiler-ignore, m2e khôi phục `org.eclipse.jdt.core.javanature`, Java builder và classpath nguồn/Maven/JavaSE-21. Bật JDT APT và bỏ source helper trùng để mapper sinh lại sau Maven clean không giữ lỗi import giả. Dọn model cũ rồi import lại cho trạng thái **No Problems**, Java Ready; `AuthService.java` được language server báo 0 lỗi. Bốn cảnh báo import không dùng trong mã test được dọn bỏ. Oracle Java được Disable Workspace riêng trên máy đang kiểm thử.

Backend `clean test`: 152 tests được liệt kê, 151 chạy thành công, 1 test schema MySQL được skip do không có MySQL ở cổng dev 3307; không có lỗi/failure. `test-compile` sau dọn import thành công. Đây là sửa cấu hình IDE/Maven, không thay đổi hợp đồng API.

Tham khảo: [m2e lifecycle mapping](https://eclipse.dev/m2e/documentation/m2e-execution-not-covered.html), [m2e giải thích ảnh hưởng của compiler ignore](https://www.eclipse.org/lists/m2e-users/msg02248.html), [MapStruct IDE support](https://mapstruct.org/documentation/ide-support/), [VS Code Java project management](https://code.visualstudio.com/docs/java/java-project).
