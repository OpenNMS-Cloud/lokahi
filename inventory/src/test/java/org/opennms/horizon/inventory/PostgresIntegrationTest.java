package org.opennms.horizon.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("inventory").withUsername("inventory")
        .withPassword("password").withExposedPorts(5432);

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @BeforeEach
    public void setup() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @Test
    void testPostgresLiquibaseMigration() throws Exception {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select * from databasechangelog")) {

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnsNumber = resultSetMetaData.getColumnCount();

            List<String> changelogs = new ArrayList<>();

            while (resultSet.next()) {
                for (int index = 1; index <= columnsNumber; index++) {
                    changelogs.add(resultSet.getString("id"));
                    assertEquals("EXECUTED", resultSet.getString("exectype"));
                }
            }
            assertTrue(changelogs.size() > 0);
        }
    }
}
