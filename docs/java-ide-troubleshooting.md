# Java trong VS Code

## Cấu hình hiện hành — 16/11/2026

Maven CLI và IDE trước đây cùng ghi `backend/target`: class, class test và nguồn MapStruct. `mvn clean` xóa đầu ra IDE; JDT có thể giữ model/chẩn đoán cũ hoặc ghi class vào lúc Maven đang biên dịch. Xung đột hai compiler dùng chung output cũng được ghi nhận tại [VS Code Java #1381](https://github.com/redhat-developer/vscode-java/issues/1381).

Đã thêm profile `ide-build` trong pom, tự kích hoạt khi có property `m2e.version` của m2e/JDT:

| Công cụ | Class / tài nguyên | Nguồn sinh |
| --- | --- | --- |
| Maven CLI, CI, Docker | `backend/target/classes`, `target/test-classes` | `target/generated-sources/annotations`, `target/generated-test-sources/test-annotations` |
| VS Code Java / m2e | `backend/.ide-build/classes`, `.ide-build/test-classes` | `.ide-build/generated-sources/annotations`, `.ide-build/generated-test-sources/test-annotations` |

Maven clean thông thường không còn xóa output IDE. Profile không đổi dependency, Java release hoặc code ứng dụng; không kích hoạt nó trong Docker. `.ide-build` được bỏ qua trong Git và Docker context. Cơ chế kích hoạt theo `m2e.version` được mô tả trong [tài liệu Eclipse m2e](https://wiki.eclipse.org/M2E-WTP_FAQ). Không dùng `java.project.outputPath` để thay thế vì setting đó không áp dụng cho Maven project.

Giữ `java.autobuild.enabled`, cập nhật build configuration tự động và Lombok support. Đã bỏ `java.errors.incompleteClasspath.severity=ignore` để vẫn thấy lỗi classpath thật, và bỏ `extensions.autoUpdate` khỏi workspace vì đó là setting cấp User/Application.

## Red Hat Java mới nhất và tự cập nhật

Chọn **Release Version**, bật auto update và không pin version. Tại thời điểm kiểm chứng, Release mới nhất là **1.58.0**, thay bản pre-release `1.59.2026111308`; log trước đó báo `BundleException: ... org.objectweb.asm ... 9.10.1` khi nạp extension bundles. Sau chuyển Release và reload, JVM language server chạy đúng 1.58.0, log khởi động mới không còn lỗi bundle này, Java test/debug extensions vẫn được giữ.

Thiết lập ở **User Settings**, không đặt trong `.vscode/settings.json`:

```json
{
  "extensions.autoCheckUpdates": true,
  "extensions.autoUpdate": "on"
}
```

Không khóa 1.58.0 trong repository; bản Release sau vẫn tự cập nhật. Không thể bảo đảm mọi extension tương lai đều không có bug. Kênh pre-release có thể chứa runtime/dependency thay đổi sớm; xem [ghi nhận tương thích của Java Test Runner](https://github.com/microsoft/vscode-java-test/pull/1882).

## Kiểm chứng chống tái diễn

- Đối chiếu classpath JDT thật: toàn bộ output/generated sources đã ở `.ide-build`, vẫn có Maven classpath, Java nature và JavaSE-21.
- Chạy `mvnw.cmd clean test` khi VS Code vẫn mở/autobuild: **168 pass, 1 test MySQL dev skip**, không reload/rebuild/cache clean sau đó; Problems vẫn 0, Java Ready.
- SHA-256 của class `RefreshTokenService.class` và nguồn `ProductMapperImpl.java` trong `.ide-build` giữ nguyên sau Maven clean.
- Thêm package/file tạm qua filesystem, không mở editor: một type sai cố ý sinh đúng một lỗi; sửa type tự về 0. Sau đó `clean test-compile` với file mới hợp lệ vẫn pass và IDE không đỏ lại; hashes IDE vẫn giữ nguyên. Đã xóa source/class thử và kiểm tra lại Problems 0.

Các phần phía dưới giữ lịch sử cấu hình và cách khôi phục khi có sự cố khác. Không cần clean cache sau mỗi lần thêm file.

## Cấu hình dự án

Backend dùng Java 21 và `maven-compiler-plugin` để cấu hình nguồn, classpath và annotation processor. Giữ mapping mặc định của m2e; không khai báo `<ignore />` cho các goal `compile`/`testCompile`. Mapping ignore khiến m2e không thêm Java nature/builder, do đó language server không nhận các package của chính dự án.

Lombok được hỗ trợ bởi Red Hat Java; MapStruct được Maven sinh trong `target/generated-sources/annotations`, JDT APT sinh riêng trong `.ide-build/generated-sources/annotations`. Đặt `m2e.apt.activation=jdt_apt` để IDE quản lý annotation processing theo `annotationProcessorPaths`. Không dùng build-helper khai báo lại thư mục sinh mà compiler đã quản lý. Sau clone hoặc khi DTO/mapper thay đổi, chạy:

```powershell
cd backend
.\mvnw.cmd test-compile
```

Các thư viện, processor và Java release tiếp tục lấy từ `pom.xml`. Không cần viết getter/constructor thủ công chỉ để làm mất lỗi IDE.

## Khôi phục khi Problems giữ lỗi cũ

Khi chỉ vừa chạy Maven clean, thử **Java: Reload Projects** rồi **Java: Rebuild Projects** trước. Nếu lỗi import vẫn nằm ở cả source cũ/mới dù classpath và Maven compiler đúng, thực hiện clean language server bên dưới. Không cần Maven clean sau mỗi lần thêm file; dùng `test-compile`/`test` cho vòng lặp thường ngày, clean khi cần kiểm chứng build sạch.

1. Sử dụng **Language Support for Java by Red Hat** và Java 21. Nếu Oracle Java đang chạy bằng JDK 17 và báo không khởi động được, đặt đúng JDK 21 cho extension đó hoặc chọn **Disable (Workspace)** khi workspace đã dùng Red Hat Java.
2. Chạy **Java: Clean Java Language Server Workspace**, chọn **Reload and delete** để IDE xóa cache và reload.
3. Chạy **Java: Import Java Projects into Workspace**, rồi **Java: Reload Projects** nếu cần. Trong phiên bản extension cũ, lệnh sau có tên **Java: Update Project Configuration**. Sau lần sửa mapping hoặc khi Maven tạo lại toàn bộ file MapStruct, có thể cần clean/reload lại để loại model Java cũ trong bộ nhớ.
4. Đợi Java báo Ready; mở lại một controller và service để xác nhận chẩn đoán. Chỉ thấy Problems bằng 0 chưa đủ: dự án phải được nhận là Java và có classpath Maven.

Việc dọn cache chỉ tác động trạng thái IDE. Không xóa mã nguồn hoặc volume Docker để sửa lỗi này.

## Kiểm chứng sửa lỗi ngày 14/11/2026

Đã xác nhận trực tiếp trong VS Code: ban đầu 346 lỗi và 4 cảnh báo; sau khi bỏ compiler-ignore, m2e khôi phục `org.eclipse.jdt.core.javanature`, Java builder và classpath nguồn/Maven/JavaSE-21. Bật JDT APT và bỏ source helper trùng để mapper sinh lại sau Maven clean không giữ lỗi import giả. Dọn model cũ rồi import lại cho trạng thái **No Problems**, Java Ready; `AuthService.java` được language server báo 0 lỗi. Bốn cảnh báo import không dùng trong mã test được dọn bỏ. Oracle Java được Disable Workspace riêng trên máy đang kiểm thử.

Backend `clean test`: 152 tests được liệt kê, 151 chạy thành công, 1 test schema MySQL được skip do không có MySQL ở cổng dev 3307; không có lỗi/failure. `test-compile` sau dọn import thành công. Đây là sửa cấu hình IDE/Maven, không thay đổi hợp đồng API.

Tham khảo: [m2e lifecycle mapping](https://eclipse.dev/m2e/documentation/m2e-execution-not-covered.html), [m2e giải thích ảnh hưởng của compiler ignore](https://www.eclipse.org/lists/m2e-users/msg02248.html), [MapStruct IDE support](https://mapstruct.org/documentation/ide-support/), [VS Code Java project management](https://code.visualstudio.com/docs/java/java-project).

## Kiểm chứng tái diễn ngày 16/11/2026

Sau khi thêm package seller và chạy build sạch, IDE giữ 204 lỗi import trong source dù backend `clean test` thành công (168 pass, 1 skip). Java nature, JavaSE-21 và classpath source/Maven vẫn có; Reload/Rebuild Projects chưa đủ. Chạy **Clean Java Language Server Workspace → Reload and delete** rồi đợi import hoàn tất đã đưa Problems về 0, Java Ready.

Đã kiểm tra nhận file mới bằng package/file probe tạm tham chiếu entity/constants có thật và một type cố ý không tồn tại: IDE báo chính xác một lỗi. Xóa type sai làm Problems tự về 0, không rebuild; sau đó xóa file probe. Đây là kiểm chứng index/chẩn đoán hoạt động. Sau build sạch phải kiểm chứng lại IDE trước khi bàn giao; No Problems tại một thời điểm không bảo đảm cache không bao giờ hỏng lại.
