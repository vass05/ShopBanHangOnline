package com.helishop.core.config;

import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.common.constants.ShopStatus;
import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductImage;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.repository.CategoryRepository;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSkuRepository;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.entity.UserAddress;
import com.helishop.core.modules.user.repository.ShopRepository;
import com.helishop.core.modules.user.repository.UserAddressRepository;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tự động nạp dữ liệu mẫu (Seed Data) cho cơ sở dữ liệu MySQL khi khởi động ứng dụng
 * Tự động tắt trong test environment nhờ app.seeder.enabled=false.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final UserAddressRepository userAddressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("==> [DatabaseSeeder] Đang kiểm tra dữ liệu khởi tạo cho hệ thống HeliShop...");

        // 1. Khởi tạo tài khoản mẫu
        User customer = initUsers();

        // 2. Khởi tạo địa chỉ nhận hàng
        initAddresses(customer);

        // 3. Khởi tạo Cây danh mục hàng hóa
        Category phoneCategory = initCategories();

        // 4. Khởi tạo các Shop chính hãng
        Shop appleShop = initShops();

        // 5. Khởi tạo Sản phẩm và các biến thể SKU 2 cấp
        initProducts(appleShop, phoneCategory);

        log.info("==> [DatabaseSeeder] Hoàn tất nạp dữ liệu mẫu cho Database thành công!");
    }

    private User initUsers() {
        if (userRepository.count() > 0) {
            return userRepository.findByEmail("customer@helishop.com").orElse(null);
        }

        log.info("[DatabaseSeeder] Tạo các tài khoản mẫu: Admin, Sellers, Customer...");
        String encodedPassword = passwordEncoder.encode("Password123!");

        User admin = User.builder()
                .email("admin@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Quản Trị Viên HeliShop")
                .phone("0901111111")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200")
                .build();
        userRepository.save(admin);

        User seller = User.builder()
                .email("seller@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Chủ Shop Apple Flagship")
                .phone("0902222222")
                .role(UserRole.ROLE_SELLER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200")
                .build();
        userRepository.save(seller);

        User sellerSony = User.builder()
                .email("seller.sony@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Chủ Shop Sony VN")
                .phone("0902222223")
                .role(UserRole.ROLE_SELLER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200")
                .build();
        userRepository.save(sellerSony);

        User sellerNuphy = User.builder()
                .email("seller.nuphy@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Chủ Shop NuPhy Studio")
                .phone("0902222224")
                .role(UserRole.ROLE_SELLER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200")
                .build();
        userRepository.save(sellerNuphy);

        User sellerCoolmate = User.builder()
                .email("seller.coolmate@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Chủ Shop Coolmate VN")
                .phone("0902222225")
                .role(UserRole.ROLE_SELLER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200")
                .build();
        userRepository.save(sellerCoolmate);

        User customer = User.builder()
                .email("customer@helishop.com")
                .passwordHash(encodedPassword)
                .fullName("Nguyễn Văn An")
                .phone("0903333333")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200")
                .build();
        return userRepository.save(customer);
    }

    private void initAddresses(User customer) {
        if (customer == null || userAddressRepository.count() > 0) return;

        UserAddress address = UserAddress.builder()
                .user(customer)
                .recipientName(customer.getFullName())
                .phone(customer.getPhone())
                .provinceCity("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Số 123 Đường Nguyễn Huệ, Tòa nhà HeliShop Tower")
                .isDefault(true)
                .build();
        userAddressRepository.save(address);
    }

    private Category initCategories() {
        if (categoryRepository.count() > 0) {
            return categoryRepository.findAll().get(0);
        }

        log.info("[DatabaseSeeder] Tạo Cây danh mục hàng hóa đa cấp...");
        Category rootTech = categoryRepository.save(Category.builder()
                .name("Điện thoại & Tablet")
                .slug("dien-thoai-tablet")
                .level(1)
                .displayOrder(1)
                .build());

        Category rootLaptop = categoryRepository.save(Category.builder()
                .name("Máy tính & Laptop")
                .slug("may-tinh-laptop")
                .level(1)
                .displayOrder(2)
                .build());

        Category rootAudio = categoryRepository.save(Category.builder()
                .name("Thiết bị Âm thanh")
                .slug("thiet-bi-am-thanh")
                .level(1)
                .displayOrder(3)
                .build());

        Category rootKeyboard = categoryRepository.save(Category.builder()
                .name("Bàn phím & Chuột")
                .slug("ban-phim-chuot")
                .level(1)
                .displayOrder(4)
                .build());

        Category rootFashion = categoryRepository.save(Category.builder()
                .name("Thời trang Nam")
                .slug("thoi-trang-nam")
                .level(1)
                .displayOrder(5)
                .build());

        // Subcategories
        Category subIphone = categoryRepository.save(Category.builder()
                .name("iPhone Chính Hãng")
                .slug("iphone-chinh-hang")
                .parent(rootTech)
                .level(2)
                .displayOrder(1)
                .build());

        return subIphone;
    }

    private Shop initShops() {
        if (shopRepository.count() > 0) {
            return shopRepository.findAll().get(0);
        }

        User seller = userRepository.findByEmail("seller@helishop.com").orElse(null);
        User sellerSony = userRepository.findByEmail("seller.sony@helishop.com").orElse(seller);
        User sellerNuphy = userRepository.findByEmail("seller.nuphy@helishop.com").orElse(seller);
        User sellerCoolmate = userRepository.findByEmail("seller.coolmate@helishop.com").orElse(seller);

        log.info("[DatabaseSeeder] Tạo các gian hàng Shopee Mall...");
        Shop appleShop = shopRepository.save(Shop.builder()
                .owner(seller)
                .shopName("Apple Flagship Store")
                .description("Gian hàng phân phối chính thức các sản phẩm Apple tại Việt Nam")
                .logoUrl("https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=300")
                .bannerUrl("https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1200")
                .rating(BigDecimal.valueOf(4.9))
                .status(ShopStatus.ACTIVE)
                .build());

        shopRepository.save(Shop.builder()
                .owner(sellerSony)
                .shopName("Sony Official Store")
                .description("Tai nghe, loa bluetooth và máy ảnh Sony chính hãng")
                .logoUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=300")
                .bannerUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200")
                .rating(BigDecimal.valueOf(4.8))
                .status(ShopStatus.ACTIVE)
                .build());

        shopRepository.save(Shop.builder()
                .owner(sellerNuphy)
                .shopName("NuPhy Mechanical Studio")
                .description("Bàn phím cơ low-profile cao cấp hàng đầu thế giới")
                .logoUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=300")
                .bannerUrl("https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=1200")
                .rating(BigDecimal.valueOf(5.0))
                .status(ShopStatus.ACTIVE)
                .build());

        shopRepository.save(Shop.builder()
                .owner(sellerCoolmate)
                .shopName("Coolmate Official Store")
                .description("Giải pháp mua sắm đồ cơ bản cho nam giới tiện lợi hơn")
                .logoUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300")
                .bannerUrl("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200")
                .rating(BigDecimal.valueOf(4.9))
                .status(ShopStatus.ACTIVE)
                .build());

        return appleShop;
    }

    private void initProducts(Shop appleShop, Category category) {
        if (appleShop == null || productRepository.count() > 0) return;

        Shop sonyShop = shopRepository.findByShopName("Sony Official Store").orElse(appleShop);
        Shop nuphyShop = shopRepository.findByShopName("NuPhy Mechanical Studio").orElse(appleShop);
        Shop coolmateShop = shopRepository.findByShopName("Coolmate Official Store").orElse(appleShop);

        Category techCategory = categoryRepository.findBySlug("dien-thoai-tablet").orElse(category);
        Category audioCategory = categoryRepository.findBySlug("thiet-bi-am-thanh").orElse(category);
        Category keyboardCategory = categoryRepository.findBySlug("ban-phim-chuot").orElse(category);
        Category laptopCategory = categoryRepository.findBySlug("may-tinh-laptop").orElse(category);
        Category fashionCategory = categoryRepository.findBySlug("thoi-trang-nam").orElse(category);

        log.info("[DatabaseSeeder] Tạo danh mục sản phẩm và các biến thể SKU 2 cấp...");

        // 1. iPhone 16 Pro Max
        Product iphone = Product.builder()
                .shop(appleShop)
                .category(techCategory)
                .name("Điện Thoại Apple iPhone 16 Pro Max 256GB - Hàng Chính Hãng VN/A")
                .slug("iphone-16-pro-max-256gb")
                .description("iPhone 16 Pro Max với thiết kế titan nguyên khối, chip Apple A18 Pro siêu mạnh mẽ, nút điều khiển Camera Control đột phá và thời lượng pin lâu nhất từng có trên iPhone.")
                .price(BigDecimal.valueOf(34990000))
                .stockQuantity(50)
                .rating(BigDecimal.valueOf(5.0))
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800")
                .build();
        Product savedIphone = productRepository.save(iphone);

        // Thêm SKU cho iPhone 16 Pro Max
        productSkuRepository.save(ProductSku.builder()
                .product(savedIphone)
                .skuCode("IP16PM-256-NATURAL")
                .price(BigDecimal.valueOf(34990000))
                .originalPrice(BigDecimal.valueOf(36990000))
                .stockQuantity(25)
                .skuAttributes("{\"color\": \"Titan Tự Nhiên\", \"storage\": \"256GB\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800")
                .build());

        productSkuRepository.save(ProductSku.builder()
                .product(savedIphone)
                .skuCode("IP16PM-512-DESERT")
                .price(BigDecimal.valueOf(40990000))
                .originalPrice(BigDecimal.valueOf(43990000))
                .stockQuantity(15)
                .skuAttributes("{\"color\": \"Titan Sa Mạc\", \"storage\": \"512GB\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800")
                .build());

        productSkuRepository.save(ProductSku.builder()
                .product(savedIphone)
                .skuCode("IP16PM-1TB-BLACK")
                .price(BigDecimal.valueOf(46990000))
                .originalPrice(BigDecimal.valueOf(49990000))
                .stockQuantity(10)
                .skuAttributes("{\"color\": \"Titan Đen\", \"storage\": \"1TB\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800")
                .build());

        // 2. Tai nghe Sony WH-1000XM5
        Product sonyHeadphone = Product.builder()
                .shop(sonyShop)
                .category(audioCategory)
                .name("Tai Nghe Chụp Tai Sony WH-1000XM5 Chống Ồn Đỉnh Cao - Pin 30H")
                .slug("sony-wh-1000xm5-chong-on")
                .description("Sony WH-1000XM5 định chuẩn khả năng chống ồn vượt bậc với 2 bộ xử lý và 8 micro chuyên dụng, chất âm Hi-Res chuẩn phòng thu.")
                .price(BigDecimal.valueOf(7990000))
                .stockQuantity(30)
                .rating(BigDecimal.valueOf(4.9))
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800")
                .build();
        Product savedSony = productRepository.save(sonyHeadphone);

        productSkuRepository.save(ProductSku.builder()
                .product(savedSony)
                .skuCode("WH1000XM5-BLACK")
                .price(BigDecimal.valueOf(7990000))
                .originalPrice(BigDecimal.valueOf(8990000))
                .stockQuantity(20)
                .skuAttributes("{\"color\": \"Đen Nhám\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800")
                .build());

        productSkuRepository.save(ProductSku.builder()
                .product(savedSony)
                .skuCode("WH1000XM5-SILVER")
                .price(BigDecimal.valueOf(7990000))
                .originalPrice(BigDecimal.valueOf(8990000))
                .stockQuantity(10)
                .skuAttributes("{\"color\": \"Bạc Bạch Kim\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800")
                .build());

        // 3. Bàn phím NuPhy Air75 V2
        Product nuphyKeyboard = Product.builder()
                .shop(nuphyShop)
                .category(keyboardCategory)
                .name("Bàn Phím Cơ Không Dây NuPhy Air75 V2 Low-Profile RGB QMK/VIA")
                .slug("nuphy-air75-v2-wireless")
                .description("Bàn phím cơ siêu mỏng NuPhy Air75 V2 với tần số quét 1000Hz 2.4Ghz, pin 4000mAh, keycap PBT doubleshot siêu bền bỉ.")
                .price(BigDecimal.valueOf(2890000))
                .stockQuantity(40)
                .rating(BigDecimal.valueOf(5.0))
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800")
                .build();
        Product savedNuphy = productRepository.save(nuphyKeyboard);

        productSkuRepository.save(ProductSku.builder()
                .product(savedNuphy)
                .skuCode("AIR75V2-RED")
                .price(BigDecimal.valueOf(2890000))
                .originalPrice(BigDecimal.valueOf(3190000))
                .stockQuantity(20)
                .skuAttributes("{\"color\": \"Trắng Xám\", \"switch\": \"Cowberry Linear (Red)\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800")
                .build());

        productSkuRepository.save(ProductSku.builder()
                .product(savedNuphy)
                .skuCode("AIR75V2-BROWN")
                .price(BigDecimal.valueOf(2890000))
                .originalPrice(BigDecimal.valueOf(3190000))
                .stockQuantity(20)
                .skuAttributes("{\"color\": \"Trắng Xám\", \"switch\": \"Moss Tactile (Brown)\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800")
                .build());

        // 4. MacBook Pro 14 M3 Pro
        Product macbook = Product.builder()
                .shop(appleShop)
                .category(laptopCategory)
                .name("Laptop Apple MacBook Pro 14 inch M3 Pro (18GB RAM / 512GB SSD) Space Black")
                .slug("macbook-pro-14-m3-pro")
                .description("MacBook Pro 14 inch trang bị chip M3 Pro với CPU 11 lõi và GPU 14 lõi mang lại hiệu năng đỉnh cao cho công việc chuyên nghiệp.")
                .price(BigDecimal.valueOf(49990000))
                .stockQuantity(20)
                .rating(BigDecimal.valueOf(5.0))
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800")
                .build();
        Product savedMacbook = productRepository.save(macbook);

        productSkuRepository.save(ProductSku.builder()
                .product(savedMacbook)
                .skuCode("MBP14-M3P-BLACK")
                .price(BigDecimal.valueOf(49990000))
                .originalPrice(BigDecimal.valueOf(52990000))
                .stockQuantity(12)
                .skuAttributes("{\"color\": \"Space Black\", \"ram\": \"18GB\", \"ssd\": \"512GB\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800")
                .build());

        // 5. Áo Polo Coolmate
        Product polo = Product.builder()
                .shop(coolmateShop)
                .category(fashionCategory)
                .name("Áo Polo Nam Coolmate ExCool Sợi Bạc Kháng Khuẩn Co Giãn Thoáng Khí")
                .slug("ao-polo-nam-coolmate-excool")
                .description("Áo polo công nghệ dệt ExCool mềm mại, thoáng mát gấp 2 lần, chống nhăn tự nhiên và thấm hút mồ hôi tối ưu.")
                .price(BigDecimal.valueOf(299000))
                .stockQuantity(200)
                .rating(BigDecimal.valueOf(4.8))
                .status(ProductStatus.ACTIVE)
                .mainImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800")
                .build();
        Product savedPolo = productRepository.save(polo);

        productSkuRepository.save(ProductSku.builder()
                .product(savedPolo)
                .skuCode("POLO-NAVY-M")
                .price(BigDecimal.valueOf(299000))
                .originalPrice(BigDecimal.valueOf(399000))
                .stockQuantity(100)
                .skuAttributes("{\"color\": \"Xanh Navy\", \"size\": \"M (55-65kg)\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800")
                .build());

        productSkuRepository.save(ProductSku.builder()
                .product(savedPolo)
                .skuCode("POLO-BLACK-L")
                .price(BigDecimal.valueOf(299000))
                .originalPrice(BigDecimal.valueOf(399000))
                .stockQuantity(100)
                .skuAttributes("{\"color\": \"Đen\", \"size\": \"L (65-75kg)\"}")
                .skuImageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800")
                .build());
    }
}
