package com.helishop.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.SQLException;

/** Hibernate update adds google_subject but does not relax the legacy MySQL NOT NULL constraint. */
@Component @RequiredArgsConstructor @Slf4j
public class GoogleIdentitySchemaMigration implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try (var connection = dataSource.getConnection()) {
            if (!"MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect Google account schema", exception);
        }
        String nullable = jdbc.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_hash'
                """, String.class);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbc.execute("ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL");
            log.info("Google account migration: password_hash accepts NULL; existing credentials retained");
        }
    }
}
