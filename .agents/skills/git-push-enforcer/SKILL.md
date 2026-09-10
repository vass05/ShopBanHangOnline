---
name: git-push-enforcer
description: >-
  Kích hoạt quy trình kiểm thử, đóng gói commit và đẩy code lên GitHub repository sau khi hoàn thành bất kỳ sprint hoặc tính năng nào.
---

# Git Push Enforcer Skill

Kỹ năng này hướng dẫn quy trình tiêu chuẩn để đảm bảo mã nguồn luôn được kiểm thử và đẩy lên remote GitHub repository trước khi kết thúc tác vụ.

## Quy trình thực hiện

1. **Chạy kiểm thử toàn diện**:
   Thực thi lệnh Maven test:
   ```powershell
   .\mvnw.cmd test
   ```
   Nếu có lỗi kiểm thử, tiến hành phân tích log và khắc phục triệt để.

2. **Kiểm tra tệp tin cần commit**:
   ```powershell
   git status
   ```
   Xem lại danh sách các file modified và untracked để chắc chắn không để sót tệp hoặc tệp rác.

3. **Tạo Commit**:
   Thực hiện add và commit bằng một lệnh trong PowerShell:
   ```powershell
   git add . ; git commit -m "feat(sprint-X): <nội dung hoàn thành>"
   ```

4. **Đẩy lên GitHub**:
   ```powershell
   git push origin main
   ```

5. **Xác nhận trạng thái đồng bộ**:
   Kiểm tra lại trạng thái sau khi push:
   ```powershell
   git status
   ```
   Đảm bảo terminal in ra: `Your branch is up to date with 'origin/main'. nothing to commit, working tree clean`.
