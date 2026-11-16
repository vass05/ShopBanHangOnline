# Tách output Maven/Java IDE — 16/11/2026

Nhánh `fix/java-build-output-isolation`, baseline `9b338f3` (PR #13). Sửa cấu hình phát triển, không thay đổi API/chức năng bán hàng.

## Vấn đề và thay đổi

Java autobuild và Maven trước đây cùng ghi/xóa `target`. Dọn cache chỉ làm hết marker tại một thời điểm; clean tiếp theo vẫn có thể làm IDE lệch model. POM nay có profile `ide-build`, tự kích hoạt bởi `m2e.version`, dùng `.ide-build` cho class/resource main/test và nguồn annotation processor. Maven CLI/CI/Docker giữ `target`; Dockerfile không cần đổi đường dẫn JAR. Generated source directories được cấu hình theo build directory để không có phần output nào còn dùng chung.

Thêm ignore `.ide-build` trong Git và Docker. Workspace giữ autobuild, automatic Maven configuration và Lombok. Bỏ setting giấu incomplete classpath và autoUpdate sai scope. Trên máy kiểm chứng, chuyển Red Hat Java pre-release về Release mới nhất 1.58.0, giữ test/debug extension, bật auto-check/update ở User Settings, metadata không pin. Đây là cấu hình máy; repository không khóa version. Log startup mới không có lỗi duplicate ASM bundle trước đó.

## Kiểm chứng

1. Maven help:evaluate bình thường trả `backend/target`; với `-Dm2e.version=validation` trả `backend/.ide-build`, outputDirectory là `.ide-build/classes`.
2. Classpath m2e thật ghi `.ide-build` cho main/test class, resource và generated source; vẫn có JavaSE-21/Maven container. Language server process chạy Red Hat 1.58.0 Release.
3. VS Code mở/autobuild trong lúc `mvnw.cmd clean test`: 169 bài liệt kê, **168 pass, 1 schema MySQL dev skip, 0 failure/error**. Không gọi Reload/Rebuild/Clean Java Workspace sau đó, Problems vẫn 0/Ready.
4. SHA-256 `RefreshTokenService.class` và `ProductMapperImpl.java` bên IDE không đổi sau Maven clean.
5. Tạo file/package test tạm bằng filesystem: IDE tự báo đúng 1 lỗi type cố ý mà không mở file; sửa tự về 0. `clean test-compile` khi file mới hợp lệ cũng pass và không ảnh hưởng hash/Problems. Đã xóa probe source/class; không commit file thử.

Hướng dẫn và nguồn tham khảo: [Java IDE](../java-ide-troubleshooting.md). Các bản extension tương lai vẫn có thể có bug; dùng kênh Release mới nhất thay vì pin hoặc hứa mọi bản pre-release đều ổn định. Khi nghi ngờ có lỗi thật, giữ diagnostics và đối chiếu Maven/compiler, không che Problems.
