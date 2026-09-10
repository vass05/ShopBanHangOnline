# Quy Tắc Bắt Buộc: Git Commit & Push Lên Remote

## Phạm vi áp dụng:
Áp dụng cho mọi tác vụ lập trình viên hoặc AI Agent thực hiện khi hoàn thành một Sprint, một Feature hoặc một Hotfix.

## Quy trình thực hiện:
1. **Kiểm thử tự động trước khi đóng gói**:
   - Chạy lệnh kiểm tra:
     ```powershell
     .\mvnw.cmd test
     ```
   - Điều kiện tiên quyết: 100% tests phải PASS. Nếu có bất kỳ test nào fail, dừng ngay lập tức và sửa lỗi trước.
2. **Kiểm tra trạng thái tệp**:
   - Chạy `git status` để xem toàn bộ danh sách tệp mới và tệp sửa đổi.
3. **Commit theo chuẩn Conventional Commits**:
   - Sử dụng dấu chấm phẩy `;` trong PowerShell:
     ```powershell
     git add . ; git commit -m "<type>(<scope>): <message>"
     ```
4. **ĐẨY NGAY LÊN GITHUB (BẮT BUỘC)**:
   - Chạy lệnh đẩy lên nhánh `main`:
     ```powershell
     git push origin main
     ```
   - Tuyệt đối không dừng lại ở local commit. Phải xác nhận git push thành công trước khi báo cáo cho người dùng.
