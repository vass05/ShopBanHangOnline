-- Only scripts/Prepare-Integration.ps1 may load this into its reserved test project.
-- Seller logins are deliberately disabled. Customers are registered through the API per test.
INSERT INTO users (id,email,password_hash,full_name,role,status,created_at,updated_at) VALUES
(9001,'e2e-seller-a@example.test','!test-seller-login-disabled','E2E Seller A','ROLE_SELLER','ACTIVE',NOW(),NOW()),
(9002,'e2e-seller-b@example.test','!test-seller-login-disabled','E2E Seller B','ROLE_SELLER','ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);
INSERT INTO categories (id,name,slug,level,display_order,created_at,updated_at) VALUES
(9001,'E2E Category','e2e-category',0,0,NOW(),NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO shops (id,owner_id,shop_name,status,created_at,updated_at) VALUES
(9001,9001,'E2E Shop A','ACTIVE',NOW(),NOW()),(9002,9002,'E2E Shop B','ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE status=VALUES(status);
INSERT INTO products (id,shop_id,category_id,name,slug,description,price,stock_quantity,status,created_at,updated_at) VALUES
(9001,9001,9001,'E2E Blue Shirt','e2e-blue-shirt','Integration fixture',125000,50,'ACTIVE',NOW(),NOW()),
(9002,9002,9001,'E2E White Shirt','e2e-white-shirt','Integration fixture',225000,50,'ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE price=VALUES(price),stock_quantity=VALUES(stock_quantity),status=VALUES(status);
INSERT INTO product_skus (id,product_id,sku_code,price,stock_quantity,sku_attributes,version,created_at,updated_at) VALUES
(9001,9001,'E2E-BLUE',125000,50,'{"Color":"Blue"}',0,NOW(),NOW()),
(9002,9002,'E2E-WHITE',225000,50,'{"Color":"White"}',0,NOW(),NOW())
ON DUPLICATE KEY UPDATE price=VALUES(price),stock_quantity=VALUES(stock_quantity);
