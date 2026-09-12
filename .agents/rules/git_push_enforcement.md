# Quy Tắc Bắt Buộc: Git Commit & Push Lên Remote Mỗi Lần Thay Đổi Code

## Phạm vi áp dụng:
Áp dụng cho **MỌI lần có sự thay đổi về mã nguồn (code)** do lập trình viên hoặc AI Agent thực hiện — bao gồm:
- Sửa lỗi nhỏ (bug fixes / hotfixes)
- Tinh chỉnh giao diện hoặc CSS (UI/UX tweaks)
- Cập nhật logic xử lý, API endpoints, DTO
- Cải tiến cấu hình dự án
- Hoàn thành từng Sprint hoặc tính năng lớn

## Quy tắc cốt lõi:
1. **Tuyệt đối không để dồn commit**: Thay đổi code xong là phải đóng gói commit và push ngay.
2. **Không được chỉ lưu ở local**: Mọi commit bắt buộc phải được đẩy lên GitHub remote `origin/main`.
3. **Phải kèm chú thích rõ ràng**: Commit message phải mô tả cụ thể, rành mạch những gì đã thay đổi.

## Quy trình thực hiện:
1. **Kiểm thử tự động trước khi đóng gói**:
   - Backend: `.\mvnw.cmd test-compile` (hoặc `.\mvnw.cmd test`)
   - Frontend: `npx tsc --noEmit`
   - Điều kiện: 100% không có lỗi biên dịch / kiểu dữ liệu.
2. **Kiểm tra trạng thái tệp**:
   - Chạy `git status` để xác nhận các tệp đã thay đổi.
3. **Commit kèm chú thích chi tiết**:
   - Cú pháp chuẩn Conventional Commits:
     ```powershell
     git add . ; git commit -m "<type>(<scope>): <chú thích chi tiết>"
     ```
4. **ĐẨY NGAY LÊN GITHUB (BẮT BUỘC)**:
   - Chạy lệnh đẩy lên nhánh `main`:
     ```powershell
     git push origin main
     ```
   - Xác nhận `git status` báo `working tree clean` và commit đã có mặt trên `origin/main`.
