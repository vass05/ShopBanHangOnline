-- ==============================================================================
-- HeliShop Core E-Commerce - Initial Database Seed Script (MySQL 8.0)
-- Database: eshop_db
-- ==============================================================================

USE eshop_db;

-- 1. USERS (Mật khẩu: Password123! được mã hóa BCrypt)
-- Hash: $2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea
INSERT INTO users (id, email, password_hash, full_name, phone, avatar_url, role, status, created_at, updated_at)
VALUES 
  (1, 'admin@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Quản Trị Viên HeliShop', '0901111111', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200', 'ROLE_ADMIN', 'ACTIVE', NOW(), NOW()),
  (2, 'seller@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Chủ Shop Apple Flagship', '0902222222', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (3, 'customer@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Nguyễn Văn An', '0903333333', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200', 'ROLE_CUSTOMER', 'ACTIVE', NOW(), NOW()),
  (4, 'seller.sony@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Sony Vietnam Store', '0904444444', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (5, 'seller.nuphy@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'NuPhy Studio VN', '0905555555', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=200', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW()),
  (6, 'seller.coolmate@helishop.com', '$2a$10$Q7yM4jQvCcm40hM15yqV7.27r9Wkvx0g5985kcm1Q45rG7eJpWJea', 'Coolmate Flagship', '0906666666', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200', 'ROLE_SELLER', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 2. USER ADDRESSES
INSERT INTO user_addresses (id, user_id, recipient_name, phone, province_city, district, ward, street_address, is_default, created_at, updated_at)
VALUES
  (1, 3, 'Nguyễn Văn An', '0903333333', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', 'Số 123 Đường Nguyễn Huệ, Tòa nhà HeliShop Tower', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE recipient_name=VALUES(recipient_name);

-- 3. SHOPS
INSERT INTO shops (id, owner_id, shop_name, description, logo_url, banner_url, rating, status, created_at, updated_at)
VALUES
  (1, 2, 'Apple Flagship Store', 'Gian hàng phân phối chính thức Apple VN', 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=300', 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1200', 4.9, 'ACTIVE', NOW(), NOW()),
  (2, 4, 'Sony Official Store', 'Tai nghe, loa bluetooth và máy ảnh Sony', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=300', 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200', 4.8, 'ACTIVE', NOW(), NOW()),
  (3, 5, 'NuPhy Mechanical Studio', 'Bàn phím cơ low-profile cao cấp', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=300', 'https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=1200', 5.0, 'ACTIVE', NOW(), NOW()),
  (4, 6, 'Coolmate Official Store', 'Thời trang cơ bản cho nam giới chất lượng cao', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300', 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200', 4.9, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE shop_name=VALUES(shop_name);

-- 4. CATEGORIES
INSERT INTO categories (id, name, slug, parent_id, level, display_order, created_at, updated_at)
VALUES
  (1, 'Điện thoại & Tablet', 'dien-thoai-tablet', NULL, 1, 1, NOW(), NOW()),
  (2, 'Máy tính & Laptop', 'may-tinh-laptop', NULL, 1, 2, NOW(), NOW()),
  (3, 'Thiết bị Âm thanh', 'thiet-bi-am-thanh', NULL, 1, 3, NOW(), NOW()),
  (4, 'Bàn phím & Chuột', 'ban-phim-chuot', NULL, 1, 4, NOW(), NOW()),
  (5, 'Thời trang Nam', 'thoi-trang-nam', NULL, 1, 5, NOW(), NOW()),
  (6, 'iPhone Chính Hãng', 'iphone-chinh-hang', 1, 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 5. PRODUCTS
INSERT INTO products (id, shop_id, category_id, name, slug, description, price, stock_quantity, rating, status, main_image_url, created_at, updated_at)
VALUES
  (1, 1, 6, 'Điện Thoại Apple iPhone 16 Pro Max 256GB - Hàng Chính Hãng VN/A', 'iphone-16-pro-max-256gb', 'Thiết kế titan nguyên khối, chip Apple A18 Pro siêu mạnh mẽ.', 34990000, 50, 5.0, 'ACTIVE', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', NOW(), NOW()),
  (2, 2, 3, 'Tai Nghe Chụp Tai Sony WH-1000XM5 Chống Ồn Đỉnh Cao - Pin 30H', 'sony-wh-1000xm5-chong-on', 'Định chuẩn khả năng chống ồn vượt bậc với 2 bộ xử lý và 8 micro.', 7990000, 30, 4.9, 'ACTIVE', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', NOW(), NOW()),
  (3, 3, 4, 'Bàn Phím Cơ Không Dây NuPhy Air75 V2 Low-Profile RGB QMK/VIA', 'nuphy-air75-v2-wireless', 'Bàn phím cơ siêu mỏng với tần số quét 1000Hz 2.4Ghz, pin 4000mAh.', 2890000, 40, 5.0, 'ACTIVE', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', NOW(), NOW()),
  (4, 1, 2, 'Laptop Apple MacBook Pro 14 inch M3 Pro Space Black', 'macbook-pro-14-m3-pro', 'Chip M3 Pro mang lại hiệu năng đỉnh cao cho công việc chuyên nghiệp.', 49990000, 20, 5.0, 'ACTIVE', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', NOW(), NOW()),
  (5, 4, 5, 'Áo Polo Nam Coolmate ExCool Sợi Bạc Kháng Khuẩn Co Giãn', 'ao-polo-nam-coolmate-excool', 'Áo polo công nghệ dệt ExCool mềm mại, thoáng mát gấp 2 lần.', 299000, 200, 4.8, 'ACTIVE', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 6. PRODUCT SKUS
INSERT INTO product_skus (id, product_id, sku_code, price, original_price, stock_quantity, sku_attributes, sku_image_url, created_at, updated_at)
VALUES
  (1, 1, 'IP16PM-256-NATURAL', 34990000, 36990000, 25, '{"color": "Titan Tự Nhiên", "storage": "256GB"}', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', NOW(), NOW()),
  (2, 1, 'IP16PM-512-DESERT', 40990000, 43990000, 15, '{"color": "Titan Sa Mạc", "storage": "512GB"}', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800', NOW(), NOW()),
  (3, 1, 'IP16PM-1TB-BLACK', 46990000, 49990000, 10, '{"color": "Titan Đen", "storage": "1TB"}', 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800', NOW(), NOW()),
  (4, 2, 'WH1000XM5-BLACK', 7990000, 8990000, 20, '{"color": "Đen Nhám"}', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', NOW(), NOW()),
  (5, 2, 'WH1000XM5-SILVER', 7990000, 8990000, 10, '{"color": "Bạc Bạch Kim"}', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800', NOW(), NOW()),
  (6, 3, 'AIR75V2-RED', 2890000, 3190000, 20, '{"color": "Trắng Xám", "switch": "Cowberry Linear"}', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', NOW(), NOW()),
  (7, 3, 'AIR75V2-BROWN', 2890000, 3190000, 20, '{"color": "Trắng Xám", "switch": "Moss Tactile"}', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', NOW(), NOW()),
  (8, 4, 'MBP14-M3P-BLACK', 49990000, 52990000, 12, '{"color": "Space Black", "ram": "18GB", "ssd": "512GB"}', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', NOW(), NOW()),
  (9, 5, 'POLO-NAVY-M', 299000, 399000, 100, '{"color": "Xanh Navy", "size": "M"}', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800', NOW(), NOW()),
  (10, 5, 'POLO-BLACK-L', 299000, 399000, 100, '{"color": "Đen", "size": "L"}', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800', NOW(), NOW())
ON DUPLICATE KEY UPDATE sku_code=VALUES(sku_code);

-- 7. VOUCHERS
INSERT INTO vouchers (id, voucher_code, name, discount_type, discount_value, min_order_value, max_discount_amount, start_date, end_date, usage_limit, used_count, status, created_at, updated_at)
VALUES
  (1, 'HELI50K', 'Giảm 50.000₫ cho đơn từ 200.000₫', 'FIXED_AMOUNT', 50000, 200000, 50000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 1000, 0, 'ACTIVE', NOW(), NOW()),
  (2, 'HELI100K', 'Giảm 100.000₫ cho đơn từ 500.000₫', 'FIXED_AMOUNT', 100000, 500000, 100000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 500, 0, 'ACTIVE', NOW(), NOW()),
  (3, 'VNPAY10', 'Giảm 10% tối đa 100.000₫ khi thanh toán VNPAY', 'PERCENTAGE', 10, 0, 100000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 2000, 0, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE voucher_code=VALUES(voucher_code);

