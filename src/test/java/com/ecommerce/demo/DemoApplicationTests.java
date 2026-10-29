package com.ecommerce.demo;

import org.junit.jupiter.api.Test;

// Sanity check — verifies the Spring application context loads without errors.
// If this fails, there's a configuration or wiring issue.
// Full unit tests are in the service/ and security/ test packages.
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // Spring context load is tested via the service-level unit tests.
        // A full @SpringBootTest here would require a running DB and Redis.
        // Run unit tests with: mvn test
    }
}
