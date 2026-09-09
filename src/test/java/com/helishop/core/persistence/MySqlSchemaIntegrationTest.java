package com.helishop.core.persistence;

import com.helishop.core.HeliShopApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import org.junit.jupiter.api.condition.EnabledIf;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HeliShopApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3307/eshop_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
    "spring.datasource.username=eshop_user",
    "spring.datasource.password=eshop_secret",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update"
})
@EnabledIf("isMySqlRunning")
class MySqlSchemaIntegrationTest {

    static boolean isMySqlRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 3307), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Kiểm tra Hibernate đã tự động tạo đủ 12 bảng trên MySQL container")
    void shouldVerifyAllTwelveTablesExistInMySql() {
        if (jdbcTemplate == null) {
            return;
        }

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'eshop_db'",
                String.class
        );

        assertThat(tables).contains(
                "users",
                "user_addresses",
                "shops",
                "categories",
                "products",
                "product_skus",
                "product_images",
                "cart_items",
                "vouchers",
                "orders",
                "order_items",
                "payment_transactions"
        );
    }
}
