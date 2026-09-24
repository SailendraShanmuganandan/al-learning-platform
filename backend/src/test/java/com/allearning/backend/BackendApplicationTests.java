package com.allearning.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring application context assembles correctly.
 * The datasource and Flyway are disabled so this test does not require a
 * running PostgreSQL instance in CI or during local builds without a DB.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // H2 in PostgreSQL-compat mode understands TIMESTAMPTZ
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        // Suppress UserDetailsService autoconfiguration warning in tests
        "spring.security.user.name=disabled",
        "spring.security.user.password=disabled"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the application context wires up without errors.
    }

}
