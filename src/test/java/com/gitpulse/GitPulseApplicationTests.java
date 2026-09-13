package com.gitpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GitPulseApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring ApplicationContext loads successfully
    }
}
