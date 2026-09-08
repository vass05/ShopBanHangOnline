package com.eshop.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3307/eshop_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
    "spring.datasource.username=eshop_user",
    "spring.datasource.password=eshop_secret",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update"
})
class MySqlSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Kiểm tra Hibernate đã tự động tạo đủ 12 bảng trên MySQL container")
    void shouldVerifyAllTwelveTablesExistInMySql() {
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
