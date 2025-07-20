package com.hxg.crApp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.github.token=test-token",
    "app.github.webhook.secret=test-secret",
    "spring.ai.openai.api-key=test-key"
})
class AiCodeReviewerApplicationTests {

    @Test
    void contextLoads() {
        // 测试Spring上下文是否能正常加载
    }
} 