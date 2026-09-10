-- ==============================================================================
-- HeliShop Core E-Commerce Platform - Comprehensive Database Seed Script
-- Target Database Engine: MySQL 8.0 / utf8mb4
-- Target Schema: eshop_db
--
-- THÔNG TIN TÀI KHOẢN MẪU:
-- 1. Khách Hàng (CUSTOMER):
--    - Họ và tên: Vũ Viết Anh
--    - Gmail: vuvietanh@gmail.com
--    - Số điện thoại: 0988889999
--    - Mật khẩu: Password123!
--    - Địa chỉ giao hàng mặc định: Thôn Tốt Động, Xã Quảng Bị, Huyện Chương Mỹ, TP. Hà Nội
--
-- 2. Quản Trị Viên (ADMIN):
--    - Email: admin.helishop@gmail.com (hoặc 0901111111)
--    - Mật khẩu: Password123!
--
-- 3. Các Chủ Gian Hàng Chính Hãng (SELLER):
--    - Apple Flagship Store: seller.apple@gmail.com (hoặc 0902222222) / Password123!
--    - Sony Official Store: seller.sony@gmail.com (hoặc 0903333333) / Password123!
--    - NuPhy Mechanical Studio: seller.nuphy@gmail.com (hoặc 0904444444) / Password123!
--    - Coolmate Official Store: seller.coolmate@gmail.com (hoặc 0905555555) / Password123!
--    - Logitech G Store: seller.logitech@gmail.com (hoặc 0906666666) / Password123!
--    - Anker Innovations Store: seller.anker@gmail.com (hoặc 0907777777) / Password123!
--
-- * Tất cả mật khẩu đã được băm bằng thuật toán BCrypt:
--   $2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea  -> "Password123!"
-- ==============================================================================

USE eshop_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. BẢNG USERS (Người dùng & Phân quyền)
-- Quy tắc: Mỗi Gmail chỉ được đăng ký duy nhất 1 tài khoản (UNIQUE constraint).
-- Hỗ trợ đăng nhập linh hoạt bằng Gmail hoặc Số điện thoại.
-- ------------------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, avatar_url, role, status, created_at, updated_at)
VALUES
  -- Khách hàng chính
  (1, 'vuvietanh@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Vũ Viết Anh', '0988889999', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300', 'ROLE_CUSTOMER', 'ACTIVE', NOW(), NOW()),
  
  -- Quản trị viên sàn HeliShop
  (2, 'admin.helishop@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Quản Trị Viên HeliShop', '0901111111', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300', 'ROLE_ADMIN', 'ACTIVE', NOW(), NOW()),
  
  -- Người bán hàng chính hãng
  (3, 'seller.apple@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Apple Flagship Store', '0902222222', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (4, 'seller.sony@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Sony Vietnam', '0903333333', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (5, 'seller.nuphy@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop NuPhy Mechanical Studio', '0904444444', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (6, 'seller.coolmate@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Coolmate Vietnam', '0905555555', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (7, 'seller.logitech@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Logitech G Vietnam', '0906666666', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (8, 'seller.anker@gmail.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Anker Innovations', '0907777777', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  full_name = VALUES(full_name),
  phone = VALUES(phone),
  password_hash = VALUES(password_hash);

-- ------------------------------------------------------------------------------
-- 2. BẢNG USER_ADDRESSES (Sổ địa chỉ nhận hàng)
-- Gán địa chỉ chuẩn xác theo yêu cầu: Vũ Viết Anh - Thôn Tốt Động, xã Quảng Bị, TP Hà Nội
-- ------------------------------------------------------------------------------
INSERT INTO user_addresses (id, user_id, recipient_name, phone, province_city, district, ward, street_address, is_default, created_at, updated_at)
VALUES
  -- Địa chỉ chính mặc định
  (1, 1, 'Vũ Viết Anh', '0988889999', 'TP. Hà Nội', 'Huyện Chương Mỹ', 'Xã Quảng Bị', 'Thôn Tốt Động', true, NOW(), NOW()),
  
  -- Địa chỉ phụ nhận hàng giờ hành chính (Văn phòng)
  (2, 1, 'Vũ Viết Anh (Văn phòng)', '0988889999', 'TP. Hà Nội', 'Quận Cầu Giấy', 'Phường Dịch Vọng Hậu', 'Tòa nhà FPT Tower, Số 10 Phố Phạm Văn Bạch', false, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  recipient_name = VALUES(recipient_name),
  street_address = VALUES(street_address),
  ward = VALUES(ward),
  district = VALUES(district),
  province_city = VALUES(province_city);

-- ------------------------------------------------------------------------------
-- 3. BẢNG SHOPS (Gian hàng Shopee Mall chính hãng)
-- Mỗi Shop sở hữu 1 owner_id riêng biệt tuân thủ ràng buộc Unique Index.
-- ------------------------------------------------------------------------------
INSERT INTO shops (id, owner_id, shop_name, description, logo_url, banner_url, rating, status, created_at, updated_at)
VALUES
  (1, 3, 'Apple Flagship Store', 'Gian hàng phân phối chính thức các sản phẩm Apple chính hãng VN/A tại Việt Nam', 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=300', 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1200', 4.98, 'ACTIVE', NOW(), NOW()),
  (2, 4, 'Sony Official Store', 'Thương hiệu âm thanh đỉnh cao, tai nghe chống ồn và máy ảnh Alpha chính hãng', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=300', 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200', 4.92, 'ACTIVE', NOW(), NOW()),
  (3, 5, 'NuPhy Mechanical Studio', 'Bàn phím cơ low-profile cao cấp, switch custom và phụ kiện công nghệ sáng tạo', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=300', 'https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=1200', 5.00, 'ACTIVE', NOW(), NOW()),
  (4, 6, 'Coolmate Official Store', 'Thương hiệu thời trang nam tối giản, công nghệ sợi vải ExCool kháng khuẩn thoáng khí', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300', 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200', 4.88, 'ACTIVE', NOW(), NOW()),
  (5, 7, 'Logitech G Official Store', 'Thiết bị ngoại vi gaming hàng đầu thế giới: chuột siêu nhẹ, phím cơ Pro và tai nghe Lightspeed', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=300', 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200', 4.95, 'ACTIVE', NOW(), NOW()),
  (6, 8, 'Anker Innovations Store', 'Giải pháp sạc nhanh GaN, pin sạc dự phòng công nghệ cao và phụ kiện số dẫn đầu toàn cầu', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=300', 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200', 4.90, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  shop_name = VALUES(shop_name),
  description = VALUES(description),
  rating = VALUES(rating);

-- ------------------------------------------------------------------------------
-- 4. BẢNG CATEGORIES (Cây danh mục hàng hóa đa cấp)
-- Cấu trúc 2 cấp (Cha - Con) hỗ trợ lọc danh mục đa tầng và breadcrumb SEO
-- ------------------------------------------------------------------------------
INSERT INTO categories (id, name, slug, parent_id, level, display_order, image_url, created_at, updated_at)
VALUES
  -- Cấp 1 (Gốc)
  (1, 'Điện Thoại & Phụ Kiện', 'dien-thoai-phu-kien', NULL, 1, 1, 'https://images.unsplash.com/photo-1511707171634-5f897ff02560?w=200', NOW(), NOW()),
  (2, 'Máy Tính & Laptop', 'may-tinh-laptop', NULL, 1, 2, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=200', NOW(), NOW()),
  (3, 'Thiết Bị Âm Thanh', 'thiet-bi-am-thanh', NULL, 1, 3, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200', NOW(), NOW()),
  (4, 'Bàn Phím & Chuột Gaming', 'ban-phim-chuot-gaming', NULL, 1, 4, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=200', NOW(), NOW()),
  (5, 'Thời Trang Nam Cao Cấp', 'thoi-trang-nam', NULL, 1, 5, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200', NOW(), NOW()),
  
  -- Cấp 2 (Danh mục con)
  (6, 'Điện Thoại Thông Minh', 'dien-thoai-thong-minh', 1, 2, 1, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=200', NOW(), NOW()),
  (7, 'Củ Sạc & Cáp Sạc Nhanh', 'cu-sac-cap-sac-nhanh', 1, 2, 2, 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=200', NOW(), NOW()),
  (8, 'Laptop Gaming & Đồ Họa', 'laptop-gaming-do-hoa', 2, 2, 1, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200', NOW(), NOW()),
  (9, 'Màn Hình Đồ Họa & Gaming', 'man-hinh-do-hoa-gaming', 2, 2, 2, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=200', NOW(), NOW()),
  (10, 'Tai Nghe Chống Ồn & Hi-Res', 'tai-nghe-chong-on-hi-res', 3, 2, 1, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200', NOW(), NOW()),
  (11, 'Bàn Phím Cơ Custom & Low-Profile', 'ban-phim-co-custom', 4, 2, 1, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=200', NOW(), NOW()),
  (12, 'Chuột Chơi Game Không Dây', 'chuot-gaming-khong-day', 4, 2, 2, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=200', NOW(), NOW()),
  (13, 'Áo Polo & Áo Thun Công Nghệ', 'ao-polo-ao-thun-cong-nghe', 5, 2, 1, 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=200', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  name = VALUES(name),
  parent_id = VALUES(parent_id);

-- ------------------------------------------------------------------------------
-- 5. BẢNG PRODUCTS (Danh mục sản phẩm chủ đạo)
-- Đầy đủ thông tin SEO, hình ảnh, rating và trạng thái kích hoạt ACTIVE
-- ------------------------------------------------------------------------------
INSERT INTO products (id, shop_id, category_id, name, slug, description, price, stock_quantity, rating, status, main_image_url, created_at, updated_at)
VALUES
  -- 1. iPhone 16 Pro Max
  (1, 1, 6, 'Điện Thoại Apple iPhone 16 Pro Max 256GB - Hàng Chính Hãng VN/A', 'iphone-16-pro-max-256gb', 
   'iPhone 16 Pro Max sở hữu khung viền Titan cấp 5 siêu nhẹ và bền bỉ. Màn hình Super Retina XDR 6.9 inch với viền mỏng nhất lịch sử Apple. Trang bị chip xử lý Apple A18 Pro tiến trình 3nm thế hệ thứ hai mang lại hiệu năng đỉnh cao, cụm 3 camera Fusion 48MP và nút Camera Control cảm ứng lực đột phá.', 
   34990000, 50, 5.00, 'ACTIVE', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', NOW(), NOW()),

  -- 2. Sony WH-1000XM5
  (2, 2, 10, 'Tai Nghe Chụp Tai Sony WH-1000XM5 Chống Ồn Đỉnh Cao - Pin 30H Hi-Res Audio', 'sony-wh-1000xm5-chong-on', 
   'Tai nghe over-ear đầu bảng từ Sony với 2 bộ xử lý và 8 micro chuyên dụng mang lại hiệu quả chống ồn ANC tốt nhất thị trường. Driver 30mm màng loa sợi carbon cho chất âm chi tiết tuyệt đối, hỗ trợ chuẩn LDAC Hi-Res Audio Wireless và thời lượng pin 30 giờ liên tục kèm sạc nhanh 3 phút được 3 giờ nghe.', 
   7990000, 30, 4.95, 'ACTIVE', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', NOW(), NOW()),

  -- 3. NuPhy Air75 V2
  (3, 3, 11, 'Bàn Phím Cơ Không Dây NuPhy Air75 V2 Low-Profile RGB QMK/VIA (1000Hz 2.4G)', 'nuphy-air75-v2-wireless', 
   'NuPhy Air75 V2 là mẫu bàn phím cơ low-profile layout 75% siêu mỏng nhẹ nhưng sở hữu tần số quét 1000Hz ở chế độ không dây 2.4Ghz. Trang bị foam tiêu âm Poron cao cấp, switch Gateron Low-profile thế hệ mới êm ái mượt mà, hỗ trợ tùy biến layout bàn phím đa nền tảng qua phần mềm QMK/VIA.', 
   2890000, 40, 5.00, 'ACTIVE', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', NOW(), NOW()),

  -- 4. MacBook Pro 14 M3 Pro
  (4, 1, 8, 'Laptop Apple MacBook Pro 14 inch M3 Pro (18GB RAM / 512GB SSD) Space Black', 'macbook-pro-14-m3-pro', 
   'MacBook Pro 14 inch thế hệ mới với màu Space Black huyền bí chống bám vân tay. Trái tim là vi xử lý Apple M3 Pro 11-core CPU và 14-core GPU với công nghệ Dynamic Caching tăng tốc render đồ họa chuyên nghiệp. Màn hình Liquid Retina XDR độ sáng 1600 nits đỉnh cao cùng thời lượng pin lên đến 22 tiếng.', 
   49990000, 20, 5.00, 'ACTIVE', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', NOW(), NOW()),

  -- 5. Áo Polo Coolmate ExCool
  (5, 4, 13, 'Áo Polo Nam Coolmate ExCool Sợi Bạc Kháng Khuẩn Co Giãn Thoáng Khí', 'ao-polo-nam-coolmate-excool', 
   'Áo polo nam công nghệ dệt sợi ExCool tích hợp ion bạc kháng khuẩn vượt trội, loại bỏ 99% vi khuẩn gây mùi. Bề mặt vải mềm mịn, thoáng khí gấp 2 lần sợi cotton truyền thống, chống nhăn tự nhiên không cần ủi và giữ form cổ áo đứng lịch lãm cả ngày.', 
   299000, 200, 4.85, 'ACTIVE', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', NOW(), NOW()),

  -- 6. Chuột Logitech G Pro X Superlight 2
  (6, 5, 12, 'Chuột Chơi Game Không Dây Siêu Nhẹ Logitech G Pro X Superlight 2 (60g, 32K DPI)', 'logitech-g-pro-x-superlight-2', 
   'Huyền thoại Esports phiên bản 2 với trọng lượng siêu nhẹ chỉ 60 gram. Cảm biến HERO 2 nâng cấp lên 32.000 DPI, tốc độ phản hồi 4000Hz (0.25ms). Công nghệ switch quang cơ lai Lightforce bền bỉ 100 triệu lần nhấn và thời lượng pin sử dụng lên đến 95 giờ liên tục.', 
   3490000, 35, 4.90, 'ACTIVE', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800', NOW(), NOW()),

  -- 7. Củ Sạc Anker Prime 67W GaN
  (7, 6, 7, 'Củ Sạc Nhanh Anker Prime 67W GaN 3 Cổng (2 Type-C + 1 USB-A, ActiveShield 2.0)', 'anker-prime-67w-gan', 
   'Củ sạc công nghệ GaN thế hệ mới nhỏ gọn hơn 51% so với củ sạc 67W thông thường. Hỗ trợ sạc nhanh đồng thời 3 thiết bị với công suất tối đa 67W qua chuẩn Power Delivery 3.0. Hệ thống cảm biến nhiệt độ thông minh ActiveShield 2.0 theo dõi nhiệt độ 3 triệu lần mỗi ngày để bảo vệ an toàn cho thiết bị.', 
   990000, 60, 4.95, 'ACTIVE', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800', NOW(), NOW()),

  -- 8. Màn Hình Dell UltraSharp U2724D 2K
  (8, 2, 9, 'Màn Hình Đồ Họa Dell UltraSharp U2724D 27 inch 2K 120Hz IPS Black 100% sRGB', 'dell-ultrasharp-u2724d-2k', 
   'Màn hình chuyên nghiệp đồ họa với tấm nền IPS Black đột phá mang lại tỷ lệ tương phản 2000:1 sâu sắc. Độ phân giải 2K QHD (2560x1440) cùng tần số quét mượt mà 120Hz. Độ phủ màu chuẩn phòng thu 100% sRGB, 98% DCI-P3 với độ sai lệch màu Delta E < 2.', 
   10890000, 15, 4.98, 'ACTIVE', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  name = VALUES(name),
  price = VALUES(price),
  stock_quantity = VALUES(stock_quantity),
  rating = VALUES(rating);

-- ------------------------------------------------------------------------------
-- 6. BẢNG PRODUCT_IMAGES (Bộ sưu tập ảnh chi tiết cho PDP Gallery)
-- Mỗi sản phẩm gồm 3-4 góc ảnh chi tiết, chỉ định ảnh đại diện is_thumbnail
-- ------------------------------------------------------------------------------
INSERT INTO product_images (id, product_id, image_url, is_thumbnail, display_order, created_at, updated_at)
VALUES
  -- iPhone 16 Pro Max Images
  (1, 1, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', true, 1, NOW(), NOW()),
  (2, 1, 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800', false, 2, NOW(), NOW()),
  (3, 1, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800', false, 3, NOW(), NOW()),
  
  -- Sony WH-1000XM5 Images
  (4, 2, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', true, 1, NOW(), NOW()),
  (5, 2, 'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800', false, 2, NOW(), NOW()),
  (6, 2, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800', false, 3, NOW(), NOW()),

  -- NuPhy Air75 V2 Images
  (7, 3, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', true, 1, NOW(), NOW()),
  (8, 3, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=800', false, 2, NOW(), NOW()),

  -- MacBook Pro 14 M3 Pro Images
  (9, 4, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', true, 1, NOW(), NOW()),
  (10, 4, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800', false, 2, NOW(), NOW()),

  -- Áo Polo Coolmate Images
  (11, 5, 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', true, 1, NOW(), NOW()),
  (12, 5, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800', false, 2, NOW(), NOW()),

  -- Logitech G Pro X Superlight 2 Images
  (13, 6, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800', true, 1, NOW(), NOW()),
  (14, 6, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800', false, 2, NOW(), NOW()),

  -- Anker Prime 67W Images
  (15, 7, 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800', true, 1, NOW(), NOW()),

  -- Dell UltraSharp U2724D Images
  (16, 8, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', true, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  image_url = VALUES(image_url),
  is_thumbnail = VALUES(is_thumbnail);

-- ------------------------------------------------------------------------------
-- 7. BẢNG PRODUCT_SKUS (Biến thể sản phẩm đa cấp)
-- Cung cấp SKU 2 cấp (Màu sắc x Dung lượng, Switch, Kích cỡ) kèm phiên bản version = 0
-- ------------------------------------------------------------------------------
INSERT INTO product_skus (id, product_id, sku_code, price, original_price, stock_quantity, sku_attributes, sku_image_url, version, created_at, updated_at)
VALUES
  -- iPhone 16 Pro Max SKUs
  (1, 1, 'IP16PM-256-NATURAL', 34990000, 36990000, 25, '{"Màu sắc": "Titan Tự Nhiên", "Dung lượng": "256GB"}', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', 0, NOW(), NOW()),
  (2, 1, 'IP16PM-512-DESERT', 40990000, 43990000, 15, '{"Màu sắc": "Titan Sa Mạc", "Dung lượng": "512GB"}', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800', 0, NOW(), NOW()),
  (3, 1, 'IP16PM-1TB-BLACK', 46990000, 49990000, 10, '{"Màu sắc": "Titan Đen", "Dung lượng": "1TB"}', 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800', 0, NOW(), NOW()),

  -- Sony WH-1000XM5 SKUs
  (4, 2, 'WH1000XM5-BLACK', 7990000, 8990000, 20, '{"Màu sắc": "Đen Nhám (Matte Black)"}', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', 0, NOW(), NOW()),
  (5, 2, 'WH1000XM5-SILVER', 7990000, 8990000, 10, '{"Màu sắc": "Bạc Bạch Kim (Platinum Silver)"}', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800', 0, NOW(), NOW()),

  -- NuPhy Air75 V2 SKUs
  (6, 3, 'AIR75V2-COWBERRY', 2890000, 3190000, 20, '{"Màu sắc": "Trắng Xám Lunar", "Loại Switch": "Cowberry Linear 45gf"}', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', 0, NOW(), NOW()),
  (7, 3, 'AIR75V2-MOSS', 2890000, 3190000, 20, '{"Màu sắc": "Trắng Xám Lunar", "Loại Switch": "Moss Tactile 60gf"}', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', 0, NOW(), NOW()),

  -- MacBook Pro 14 M3 Pro SKUs
  (8, 4, 'MBP14-M3P-BLACK', 49990000, 52990000, 12, '{"Màu sắc": "Space Black", "Cấu hình": "18GB RAM / 512GB SSD"}', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', 0, NOW(), NOW()),
  (9, 4, 'MBP14-M3P-SILVER', 49990000, 52990000, 8, '{"Màu sắc": "Silver", "Cấu hình": "18GB RAM / 512GB SSD"}', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800', 0, NOW(), NOW()),

  -- Coolmate Polo SKUs
  (10, 5, 'POLO-NAVY-M', 299000, 399000, 50, '{"Màu sắc": "Xanh Navy", "Kích cỡ": "M (55-65kg)"}', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', 0, NOW(), NOW()),
  (11, 5, 'POLO-NAVY-L', 299000, 399000, 50, '{"Màu sắc": "Xanh Navy", "Kích cỡ": "L (65-75kg)"}', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', 0, NOW(), NOW()),
  (12, 5, 'POLO-BLACK-M', 299000, 399000, 50, '{"Màu sắc": "Đen Huyền Bí", "Kích cỡ": "M (55-65kg)"}', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800', 0, NOW(), NOW()),
  (13, 5, 'POLO-BLACK-L', 299000, 399000, 50, '{"Màu sắc": "Đen Huyền Bí", "Kích cỡ": "L (65-75kg)"}', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800', 0, NOW(), NOW()),

  -- Logitech G Pro X Superlight 2 SKUs
  (14, 6, 'GPX2-BLACK', 3490000, 3890000, 20, '{"Màu sắc": "Đen Nhám"}', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800', 0, NOW(), NOW()),
  (15, 6, 'GPX2-WHITE', 3490000, 3890000, 15, '{"Màu sắc": "Trắng Tinh Khôi"}', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800', 0, NOW(), NOW()),

  -- Anker Prime 67W SKUs
  (16, 7, 'ANKER-67W-BLACK', 990000, 1250000, 40, '{"Màu sắc": "Đen Kim Loại Titanium"}', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800', 0, NOW(), NOW()),
  (17, 7, 'ANKER-67W-SILVER', 990000, 1250000, 20, '{"Màu sắc": "Bạc Ánh Kim"}', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800', 0, NOW(), NOW()),

  -- Dell UltraSharp U2724D SKUs
  (18, 8, 'DELL-U2724D-STAND', 10890000, 11990000, 15, '{"Phiên bản": "Kèm Chân Đế Công Thái Học Tùy Chỉnh"}', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  sku_code = VALUES(sku_code),
  price = VALUES(price),
  stock_quantity = VALUES(stock_quantity);

-- ------------------------------------------------------------------------------
-- 8. BẢNG VOUCHERS (Mã khuyến mãi toàn sàn & Shopee Mall)
-- ------------------------------------------------------------------------------
INSERT INTO vouchers (id, voucher_code, name, discount_type, discount_value, min_order_value, max_discount_amount, start_date, end_date, usage_limit, used_count, status, created_at, updated_at)
VALUES
  (1, 'HELI50K', 'Giảm 50.000₫ cho đơn hàng từ 200.000₫', 'FIXED_AMOUNT', 50000, 200000, 50000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 1000, 0, 'ACTIVE', NOW(), NOW()),
  (2, 'HELI100K', 'Giảm 100.000₫ cho đơn hàng từ 500.000₫', 'FIXED_AMOUNT', 100000, 500000, 100000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 500, 0, 'ACTIVE', NOW(), NOW()),
  (3, 'HELI500K', 'Voucher độc quyền HeliShop Super Brand Day giảm 500.000₫', 'FIXED_AMOUNT', 500000, 5000000, 500000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 200, 0, 'ACTIVE', NOW(), NOW()),
  (4, 'VNPAY10', 'Giảm ngay 10% (Tối đa 100.000₫) khi thanh toán qua cổng VNPAY-QR', 'PERCENTAGE', 10, 0, 100000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 5000, 0, 'ACTIVE', NOW(), NOW()),
  (5, 'FREESHIP', 'Mã Miễn Phí Vận Chuyển toàn quốc cho đơn từ 150.000₫', 'FIXED_AMOUNT', 30000, 150000, 30000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 10000, 0, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  voucher_code = VALUES(voucher_code),
  discount_value = VALUES(discount_value);

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- HOÀN TẤT NẠP DỮ LIỆU SEED CHO HELISHOP E-COMMERCE DATABASE
-- ==============================================================================
