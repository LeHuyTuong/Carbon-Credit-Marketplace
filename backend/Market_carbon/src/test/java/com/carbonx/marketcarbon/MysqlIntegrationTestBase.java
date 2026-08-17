package com.carbonx.marketcarbon;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * P1.2: base class for integration tests against REAL MySQL 8 (Testcontainers).
 *
 * Design decisions:
 * - ONE container per JVM (singleton pattern): container start (~10-20s) is paid once,
 *   all IT classes reuse it; schema is recreated per Spring context (ddl-auto=create-drop
 *   in application-test.properties).
 * - MySQL 8 — same major version as production. H2 was rejected: it cannot demonstrate
 *   InnoDB row locks, REPEATABLE READ behavior, deadlock detection or DECIMAL semantics,
 *   which is exactly what P1.3/P1.4 must prove.
 * - Multi-threaded tests CANNOT use Spring's test-transaction rollback: the worker threads
 *   open their own transactions/connections. Isolation is therefore explicit: tests create
 *   their own rows and clean them in @AfterEach (see the concurrency tests).
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class MysqlIntegrationTestBase {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("core_ccm_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start(); // singleton — started once per test JVM, reused across IT classes
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}
