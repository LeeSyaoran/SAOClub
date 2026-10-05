package com.example.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test - requires real running SQL Server instance")
class BackEndApplicationTests {

    @Test
    void contextLoads() {
    }

}
