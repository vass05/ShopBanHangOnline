---
name: project-roadmap-manager
description: >-
  Quản lý lộ trình dự án HeliShop, chuyển giao giữa các sprint, cập nhật tài liệu bộ nhớ ROADMAP.md và MEMORY.md, đồng thời kiểm tra Definition of Done (DoD).
---

# Project Roadmap Manager Skill

Kỹ năng này chịu trách nhiệm điều phối tiến độ của 6 Sprint thuộc Project 2 (và các dự án tiếp theo), đảm bảo tài liệu hóa và cập nhật trạng thái liên tục.

## Các bước thực hiện khi bắt đầu Sprint mới:

1. **Đọc tài liệu lộ trình**:
   Xem tệp [docs/ROADMAP.md](file:///d:/D%E1%BB%B1%20%C3%A1n%20c%C3%A1%20nh%C3%A2n/Web%20b%C3%A1n%20h%C3%A0ng%20online/docs/ROADMAP.md) và kế hoạch chi tiết của Sprint tương ứng trong `docs/sprints/`.
2. **Lập Implementation Plan**:
   Tạo kế hoạch chi tiết trong artifact `implementation_plan.md` theo định dạng chuẩn của Antigravity.
3. **Triển khai mã nguồn**:
   - Tuân thủ quy chuẩn mã nguồn và kiến trúc layered domain.
   - Viết unit test và integration test kèm theo.
4. **Kiểm tra tiêu chí hoàn thành (Definition of Done - DoD)**:
   - Toàn bộ tính năng trong sprint hoạt động trơn tru.
   - Swagger OpenAPI được cập nhật.
   - Test suite chạy 100% thành công không có lỗi hồi quy.
5. **Cập nhật tài liệu & Đẩy lên GitHub**:
   - Cập nhật trạng thái trong `docs/ROADMAP.md` và `docs/MEMORY.md`.
   - Viết bản tổng kết trong `docs/sprints/`.
   - Thực thi skill `git-push-enforcer`.
