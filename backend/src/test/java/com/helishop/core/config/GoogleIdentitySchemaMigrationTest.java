package com.helishop.core.config;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.sql.*;
import static org.mockito.Mockito.*;

class GoogleIdentitySchemaMigrationTest {
    DataSource source; JdbcTemplate jdbc; Connection connection; DatabaseMetaData metadata;
    @BeforeEach void setup() throws Exception {
        source = mock(DataSource.class); jdbc = mock(JdbcTemplate.class); connection = mock(Connection.class); metadata = mock(DatabaseMetaData.class);
        when(source.getConnection()).thenReturn(connection); when(connection.getMetaData()).thenReturn(metadata);
    }
    @Test void oldMysqlConstraintIsRelaxedWithoutUpdatingPasswordsOrUsers() throws Exception {
        when(metadata.getDatabaseProductName()).thenReturn("MySQL"); when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("NO");
        new GoogleIdentitySchemaMigration(source, jdbc).run(null);
        verify(jdbc).execute("ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL");
        verify(jdbc, never()).update(anyString());
    }
    @Test void migrationIsIdempotentAndOtherDatabasesAreUnchanged() throws Exception {
        when(metadata.getDatabaseProductName()).thenReturn("MySQL"); when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("YES");
        new GoogleIdentitySchemaMigration(source, jdbc).run(null); verify(jdbc, never()).execute(anyString());
        clearInvocations(jdbc); when(metadata.getDatabaseProductName()).thenReturn("H2");
        new GoogleIdentitySchemaMigration(source, jdbc).run(null); verifyNoInteractions(jdbc);
    }
}
