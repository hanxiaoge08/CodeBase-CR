package com.hxg.service.impl;

import com.hxg.llm.service.LlmService;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.service.IMemoryIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CatalogueServiceImpl 测试类
 * 
 * @author hxg
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogueServiceImpl 测试")
class CatalogueServiceImplTest {

    @Mock
    private LlmService llmService;
    
    @Mock
    private IMemoryIntegrationService memoryIntegrationService;

    private CatalogueServiceImpl catalogueService;

    @BeforeEach
    void setUp() {
        catalogueService = new CatalogueServiceImpl(llmService,memoryIntegrationService);
    }

    @Test
    @DisplayName("测试解析包含 documentation_structure 包装的 JSON")
    void testProcessCatalogueStructWithWrapper() {
        // Given - 您提供的 JSON 格式
        String jsonWithWrapper = """
            {
                "documentation_structure": {
                    "items": [
                        {
                            "title": "system-architecture",
                            "name": "系统架构",
                            "dependent_file": [
                                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/config/MybatisPlusConfig.java",
                                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/config/ThreadPoolConfig.java",
                                "jdeepwiki/src/main/resources/application.yml"
                            ],
                            "prompt": "创建一个全面的内容部分，重点介绍系统的整体架构和组件关系。解释使用的架构决策和模式。可视化系统边界和集成点。",
                            "children": [
                                {
                                    "title": "data-models-persistence",
                                    "name": "数据模型与持久化",
                                    "dependent_file": [
                                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/mapper/CatalogueMapper.java",
                                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/model/entity/Catalogue.java",
                                        "jdeepwiki/src/main/resources/schema.sql"
                                    ],
                                    "prompt": "文档数据库模式、数据模型和实体关系。解释数据迁移和版本控制策略。讨论数据访问模式和优化技术。"
                                }
                            ]
                        },
                        {
                            "title": "api-integration",
                            "name": "API与集成",
                            "dependent_file": [
                                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/ChatController.java",
                                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/TaskController.java"
                            ],
                            "prompt": "文档所有公共API端点、参数和响应。解释外部系统集成模式。解决速率限制、缓存和性能考虑因素。",
                            "children": [
                                {
                                    "title": "chat-controller",
                                    "name": "聊天控制器",
                                    "dependent_file": [
                                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/ChatController.java"
                                    ],
                                    "prompt": "开发详细的子部分内容，涵盖聊天控制器的具体方面。"
                                }
                            ]
                        }
                    ]
                }
            }
            """;

        // When
        CatalogueStruct result = catalogueService.processCatalogueStruct(jsonWithWrapper);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());

        // 验证第一个项目
        CatalogueStruct.Item firstItem = result.getItems().get(0);
        assertEquals("system-architecture", firstItem.getTitle());
        assertEquals("系统架构", firstItem.getName());
        assertEquals(3, firstItem.getDependent_file().size());
        assertNotNull(firstItem.getChildren());
        assertEquals(1, firstItem.getChildren().size());

        // 验证第二个项目
        CatalogueStruct.Item secondItem = result.getItems().get(1);
        assertEquals("api-integration", secondItem.getTitle());
        assertEquals("API与集成", secondItem.getName());
        assertEquals(2, secondItem.getDependent_file().size());
        assertNotNull(secondItem.getChildren());
        assertEquals(1, secondItem.getChildren().size());
    }

    @Test
    @DisplayName("测试解析不包含包装的 JSON（向后兼容）")
    void testProcessCatalogueStructWithoutWrapper() {
        // Given - 直接的 CatalogueStruct 格式
        String jsonWithoutWrapper = """
            {
                "items": [
                    {
                        "title": "test-section",
                        "name": "测试章节",
                        "dependent_file": [
                            "test/file.java"
                        ],
                        "prompt": "测试提示",
                        "children": []
                    }
                ]
            }
            """;

        // When
        CatalogueStruct result = catalogueService.processCatalogueStruct(jsonWithoutWrapper);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());

        CatalogueStruct.Item item = result.getItems().get(0);
        assertEquals("test-section", item.getTitle());
        assertEquals("测试章节", item.getName());
        assertEquals(1, item.getDependent_file().size());
        assertEquals("test/file.java", item.getDependent_file().get(0));
    }

    @Test
    @DisplayName("测试解析空项目列表时抛出异常")
    void testProcessCatalogueStructWithEmptyItems() {
        // Given
        String jsonWithEmptyItems = """
            {
                "documentation_structure": {
                    "items": []
                }
            }
            """;

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> catalogueService.processCatalogueStruct(jsonWithEmptyItems));

        assertTrue(exception.getMessage().contains("LLM生成的目录为空"));
    }

    @Test
    @DisplayName("测试解析无效 JSON 时抛出异常")
    void testProcessCatalogueStructWithInvalidJson() {
        // Given
        String invalidJson = "{ invalid json }";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> catalogueService.processCatalogueStruct(invalidJson));

        assertTrue(exception.getMessage().contains("LLM生成的目录格式不正确"));
    }
} 
//prompt="""
//<documentation_structure>
//{
//    "items": [
//        {
//            "title": "introduction",
//            "name": "项目简介",
//            "dependent_file": [
//                "README.md"
//            ],
//            "prompt": "介绍JDeepWiki项目的整体目标、背景和用途。解释其主要功能和适用场景。",
//            "children": []
//        },
//        {
//            "title": "getting_started",
//            "name": "快速开始",
//            "dependent_file": [
//                "jdeepwiki/pom.xml",
//                "jdeepwiki-frontend/package.json"
//            ],
//            "prompt": "创建一个指南，帮助用户安装和运行JDeepWiki项目。包括必要的环境配置和依赖项说明。",
//            "children": []
//        },
//        {
//            "title": "system_architecture",
//            "name": "系统架构概述",
//            "dependent_file": [
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/JdeepwikiApplication.java",
//                "jdeepwiki/src/main/resources/application.yml"
//            ],
//            "prompt": "文档应包含系统的总体设计、组件关系以及所使用的架构决策和模式。",
//            "children": [
//                {
//                    "title": "web_application",
//                    "name": "Web应用结构",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/ChatController.java",
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/TaskController.java"
//                    ],
//                    "prompt": "详细描述Web应用的实现细节，如控制器、路由和服务层。解释它们如何与前端交互。"
//                },
//                {
//                    "title": "microservices_integration",
//                    "name": "微服务集成",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/service/CatalogueService.java",
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/service/TaskService.java"
//                    ],
//                    "prompt": "讨论服务之间的集成方式及其实现细节，例如CatalogueService和TaskService之间的交互。"
//                }
//            ]
//        },
//        {
//            "title": "deployment_infrastructure",
//            "name": "部署与基础设施",
//            "dependent_file": [
//                "jdeepwiki/src/main/resources/application.yml"
//            ],
//            "prompt": "文档应包含容器化、编排和扩展的方法，以及环境配置和基础设施需求。",
//            "children": [
//                {
//                    "title": "environment_configuration",
//                    "name": "环境配置",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/resources/application.yml"
//                    ],
//                    "prompt": "详细介绍如何根据不同的环境（开发、测试、生产）进行配置。"
//                }
//            ]
//        },
//        {
//            "title": "data_models_persistence",
//            "name": "数据模型与持久化",
//            "dependent_file": [
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/mapper/CatalogueMapper.java",
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/mapper/TaskMapper.java",
//                "data/jdeepwiki_db.sqlite"
//            ],
//            "prompt": "记录数据库模式、数据模型和实体关系，并解释数据迁移和版本控制策略。",
//            "children": [
//                {
//                    "title": "database_schemas",
//                    "name": "数据库模式",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/resources/schema.sql"
//                    ],
//                    "prompt": "提供详细的数据库模式定义及其与代码中实体类的关系。"
//                }
//            ]
//        },
//        {
//            "title": "authentication_security",
//            "name": "认证与安全",
//            "dependent_file": [
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/filter/MDCFilter.java",
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/exception/GlobalExceptionHandler.java"
//            ],
//            "prompt": "记录认证流程和授权机制，解释安全特性、加密和数据保护措施。",
//            "children": [
//                {
//                    "title": "global_exception_handling",
//                    "name": "全局异常处理",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/exception/GlobalExceptionHandler.java"
//                    ],
//                    "prompt": "描述全局异常处理机制如何捕获和处理错误。"
//                }
//            ]
//        },
//        {
//            "title": "api_integration",
//            "name": "API与集成",
//            "dependent_file": [
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/ChatController.java",
//                "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/TaskController.java"
//            ],
//            "prompt": "记录所有公共API端点、参数和响应，解释与其他外部系统的集成模式。",
//            "children": [
//                {
//                    "title": "chat_api",
//                    "name": "聊天API",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/ChatController.java"
//                    ],
//                    "prompt": "提供关于聊天API的具体接口说明和使用示例。"
//                },
//                {
//                    "title": "task_api",
//                    "name": "任务API",
//                    "dependent_file": [
//                        "jdeepwiki/src/main/java/com/d1nvan/jdeepwiki/controller/TaskController.java"
//                    ],
//                    "prompt": "详细描述任务API的功能、参数和返回值。"
//                }
//            ]
//        },
//        {
//            "title": "user_interface_components",
//            "name": "用户界面组件",
//            "dependent_file": [
//                "jdeepwiki-frontend/src/components/AddRepoModal.jsx",
//                "jdeepwiki-frontend/src/pages/HomePage.jsx"
//            ],
//            "prompt": "记录UI架构、组件层次结构和状态管理方法，解释主题、样式和响应式设计方法。",
//            "children": [
//                {
//                    "title": "component_hierarchy",
//                    "name": "组件层次结构",
//                    "dependent_file": [
//                        "jdeepwiki-frontend/src/layouts/MainLayout.jsx"
//                    ],
//                    "prompt": "描述前端的主要组件及其相互关系。"
//                }
//            ]
//        },
//        {
//            "title": "testing_quality_assurance",
//            "name": "测试与质量保证",
//            "dependent_file": [],
//            "prompt": "记录测试策略、框架和覆盖率，解释CI/CD管道集成和自动化测试方法。",
//            "children": []
//        },
//        {
//            "title": "troubleshooting_advanced_usage",
//            "name": "故障排除与高级用法",
//            "dependent_file": [],
//            "prompt": "提供常见问题的解决方案和高级使用技巧。",
//            "children": []
//        }
//    ]
//}
//</documentation_structure>
//"""