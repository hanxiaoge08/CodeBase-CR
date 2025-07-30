package com.hxg.crApp.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAGService 测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.knowledge.index-name=test-knowledge-base"
})
class RAGServiceTest {

    @Autowired
    private RAGService ragService;

    @Test
    void testRetrieveContext() {
        // 测试代码片段
        String codeSnippet = """
            public class UserService {
                public User findById(Long id) {
                    return userRepository.findById(id);
                }
            }
            """;

        String repoFullName = "test/repo";

        // 执行检索
        String context = ragService.retrieveContext(codeSnippet, repoFullName);

        // 验证结果
        assertNotNull(context);
        assertFalse(context.isEmpty());
//        assertTrue(context.contains("编码规范"));
//        assertTrue(context.contains("代码示例"));
        
        System.out.println("检索到的上下文:");
        System.out.println(context);
    }

    @Test
    void testRetrieveContextWithEmptyInput() {
        String context = ragService.retrieveContext("", "test/repo");
        
        assertNotNull(context);
        // 即使输入为空，也应该返回一些基础的上下文信息
    }

    @Test
    void testRetrieveContextWithNullInput() {
        String context = ragService.retrieveContext(null, "test/repo");
        
        assertNotNull(context);
        // 应该处理null输入，返回错误信息或默认上下文
    }
} 