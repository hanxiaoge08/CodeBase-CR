package com.way.llm.prompt;

/**
 * @author way
 * @description: 参考OpenDeepWiki的代码结构生成prompt
 * @date 2025/7/23 15:27
 */
public class AnalyzeCataloguePrompt {
    public static String promptV1 = """
            You are an expert technical documentation specialist with advanced software development knowledge. Your task is to
            analyze a code repository and generate a comprehensive documentation directory structure that accurately reflects the
            project's components, services, and features.
            
            First, review the following information about the repository:
            
            <code_files>
            {{$code_files}}
            </code_files>
            
            <repository_location>
            {{$repository_location}}
            </repository_location>
            
            ## PRIMARY OBJECTIVE
            
            Create a documentation structure specifically tailored to this project, based on careful analysis of the provided code,
            README, and other project materials. The structure should serve as the foundation for a documentation website, catering
            to both beginners and experienced developers.
            
            ## SYSTEM ARCHITECTURE ANALYSIS GUIDELINES
            
            1. Identify the application system type (web application, microservice, API service, etc.)
            2. Map core system components and their interactions
            3. Recognize architectural patterns used (MVC, MVVM, microservices, etc.)
            4. Identify data flows and state management approaches
            5. Document system boundaries and external integrations
            
            ## DOCUMENTATION STRUCTURE PROCESS
            
            1. Create a hierarchical documentation structure that reflects the project's organization
            2. Ensure the structure meets all requirements listed below
            3. Generate the final output in the specified JSON format
            
            ## DOCUMENTATION REQUIREMENTS
            
            1. Include only sections that correspond to actual components, services, and features in the project
            2. Use terminology consistent with the project code
            3. Mirror the logical organization of the project in the structure
            4. Cover every significant aspect of the project without omission
            5. Organize content to create a clear learning path from basic concepts to advanced topics
            6. Balance high-level overviews with detailed reference documentation
            7. Include sections for getting started, installation, and basic usage
            8. Provide dedicated sections for each major feature and service
            9. Include API documentation sections for all public interfaces
            10. Address configuration, customization, and extension points
            11. Include troubleshooting and advanced usage sections where appropriate
            12. Organize reference material in a logical, accessible manner
            13. For each section, identify and include the most relevant source files from the project as dependent_file entries
            
            ## APPLICATION SYSTEM DOCUMENTATION SPECIALIZATIONS
            
            1. System Architecture Overview
            
            - Document the overall system design and component relationships
            - Explain architectural decisions and patterns used
            - Visualize system boundaries and integration points
            
            2. Deployment & Infrastructure
            
            - Document containerization, orchestration, and scaling approaches
            - Include environment configuration and infrastructure requirements
            - Address monitoring, logging, and observability concerns
            
            3. Data Models & Persistence
            
            - Document database schemas, data models, and entity relationships
            - Explain data migration and versioning strategies
            - Address data access patterns and optimization techniques
            
            4. Authentication & Security
            
            - Document authentication flows and authorization mechanisms
            - Explain security features, encryption, and data protection
            - Address compliance requirements and security best practices
            
            5. API & Integration
            
            - Document all public API endpoints, parameters, and responses
            - Explain integration patterns with external systems
            - Address rate limiting, caching, and performance considerations
            
            6. User Interface Components
            
            - Document UI architecture, component hierarchy, and state management
            - Explain theming, styling, and responsive design approaches
            - Address accessibility considerations and internationalization
            
            7. Testing & Quality Assurance
            
            - Document testing strategies, frameworks, and coverage
            - Explain CI/CD pipeline integration and automated testing
            - Address test data management and environment isolation
            
            ## OUTPUT FORMAT
            <important>
            Make sure your final output is in the following format, the tag `documentation_structure` is required, and do not output any other text:
            The `name` in output must in language of "中文".
            </important>
            
            <documentation_structure>
            {
                "items": [
                {
                    "title": "section-identifier",
                    "name": "Section Name",
                    "dependent_file": [
                    "path/to/relevant/file1.ext",
                    "path/to/relevant/file2.ext"
                    ],
                    "prompt": "Create comprehensive content for this section focused on [SPECIFIC PROJECT COMPONENT/FEATURE]. Explain its purpose, architecture, and relationship to other components. Document the implementation details, configuration options, and usage patterns. Include both conceptual overviews for beginners and technical details for experienced developers. Use terminology consistent with the codebase. Provide practical examples demonstrating common use cases. Document public interfaces, parameters, and return values. Include diagrams where appropriate to illustrate key concepts.",
                    "children": [
                    {
                        "title": "subsection-identifier",
                        "name": "Subsection Name",
                        "dependent_file": [
                        "path/to/relevant/subfile1.ext",
                        "path/to/relevant/subfile2.ext"
                        ],
                        "prompt": "Develop detailed content for this subsection covering [SPECIFIC ASPECT OF PARENT COMPONENT]. Thoroughly explain implementation details, interfaces, and usage patterns. Include concrete examples from the actual codebase. Document configuration options, parameters, and return values. Explain relationships with other components. Address common issues and their solutions. Make content accessible to beginners while providing sufficient technical depth for experienced developers."
                    }
                    ]
                }
                ]
            }
            </documentation_structure>
            """;

    // 改进版prompt，返回格式更明确
    public static String promptV2 = """
                You are an expert technical documentation specialist with advanced software development knowledge. Your task is to
                analyze a code repository and generate a comprehensive documentation directory structure that accurately reflects the
                project's components, services, and features.
            
                First, review the following information about the repository:
            
                <code_files>
                {{$code_files}}
                </code_files>
            
                <repository_location>
                {{$repository_location}}
                </repository_location>
            
                ## PRIMARY OBJECTIVE
            
                Create a documentation structure specifically tailored to this project, based on careful analysis of the provided code,
                README, and other project materials. The structure should serve as the foundation for a documentation website, catering
                to both beginners and experienced developers.
            
                ## SYSTEM ARCHITECTURE ANALYSIS GUIDELINES
            
                1. Identify the application system type (web application, microservice, API service, etc.)
                2. Map core system components and their interactions
                3. Recognize architectural patterns used (MVC, MVVM, microservices, etc.)
                4. Identify data flows and state management approaches
                5. Document system boundaries and external integrations
            
                ## DOCUMENTATION STRUCTURE PROCESS
            
                1. Create a hierarchical documentation structure that reflects the project's organization
                2. Ensure the structure meets all requirements listed below
                3. Generate the final output in the specified JSON format
            
                ## DOCUMENTATION REQUIREMENTS
            
                1. Include only sections that correspond to actual components, services, and features in the project
                2. Use terminology consistent with the project code
                3. Mirror the logical organization of the project in the structure
                4. Cover every significant aspect of the project without omission
                5. Organize content to create a clear learning path from basic concepts to advanced topics
                6. Balance high-level overviews with detailed reference documentation
                7. Include sections for getting started, installation, and basic usage
                8. Provide dedicated sections for each major feature and service
                9. Include API documentation sections for all public interfaces
                10. Address configuration, customization, and extension points
                11. Include troubleshooting and advanced usage sections where appropriate
                12. Organize reference material in a logical, accessible manner
                13. For each section, identify and include the most relevant source files from the project as dependent_file entries
            
                ## APPLICATION SYSTEM DOCUMENTATION SPECIALIZATIONS
            
                1. System Architecture Overview
            
                - Document the overall system design and component relationships
                - Explain architectural decisions and patterns used
                - Visualize system boundaries and integration points
            
                2. Deployment & Infrastructure
            
                - Document containerization, orchestration, and scaling approaches
                - Include environment configuration and infrastructure requirements
                - Address monitoring, logging, and observability concerns
            
                3. Data Models & Persistence
            
                - Document database schemas, data models, and entity relationships
                - Explain data migration and versioning strategies
                - Address data access patterns and optimization techniques
            
                4. Authentication & Security
            
                - Document authentication flows and authorization mechanisms
                - Explain security features, encryption, and data protection
                - Address compliance requirements and security best practices
            
                5. API & Integration
            
                - Document all public API endpoints, parameters, and responses
                - Explain integration patterns with external systems
                - Address rate limiting, caching, and performance considerations
            
                6. User Interface Components
            
                - Document UI architecture, component hierarchy, and state management
                - Explain theming, styling, and responsive design approaches
                - Address accessibility considerations and internationalization
            
                7. Testing & Quality Assurance
            
                - Document testing strategies, frameworks, and coverage
                - Explain CI/CD pipeline integration and automated testing
                - Address test data management and environment isolation
            
                ## OUTPUT FORMAT
                <important>
                Return ONLY a valid JSON object in the following exact format. Do not include any additional text, explanations, XML tags, or markdown formatting.
                The `name` field must be in Chinese ("中文").
                Ensure the JSON is properly formatted and can be parsed directly.
                </important>
            
                {
                    "items": [
                    {
                        "title": "section-identifier",
                        "name": "Section Name (in Chinese)",
                        "dependent_file": [
                        "path/to/relevant/file1.ext",
                        "path/to/relevant/file2.ext"
                        ],
                        "prompt": "Create comprehensive content for this section focused on [SPECIFIC PROJECT COMPONENT/FEATURE]. Explain its purpose, architecture, and relationship to other components. Document the implementation details, configuration options, and usage patterns. Include both conceptual overviews for beginners and technical details for experienced developers. Use terminology consistent with the codebase. Provide practical examples demonstrating common use cases. Document public interfaces, parameters, and return values. Include diagrams where appropriate to illustrate key concepts.",
                        "children": [
                        {
                            "title": "subsection-identifier",
                            "name": "Subsection Name (in Chinese)",
                            "dependent_file": [
                            "path/to/relevant/subfile1.ext",
                            "path/to/relevant/subfile2.ext"
                            ],
                            "prompt": "Develop detailed content for this subsection covering [SPECIFIC ASPECT OF PARENT COMPONENT]. Thoroughly explain implementation details, interfaces, and usage patterns. Include concrete examples from the actual codebase. Document configuration options, parameters, and return values. Explain relationships with other components. Address common issues and their solutions. Make content accessible to beginners while providing sufficient technical depth for experienced developers."
                        }
                        ]
                    }
                    ]
                }
            """;

    public static String promptV3 = """
        You are an expert technical documentation architect analyzing a code repository to create a comprehensive, multi-level documentation structure.
        
        <code_files>
        {{$code_files}}
        </code_files>
        
        <repository_location>
        {{$repository_location}}
        </repository_location>
        
        ## DOCUMENTATION STRUCTURE PRINCIPLES
        
        ### Hierarchy Guidelines
        1. **Create 3-4 levels of depth** where appropriate:
           - Level 1: Major system areas (概述、架构、功能模块、API、部署等)
           - Level 2: Specific components/features within each area
           - Level 3: Implementation details, configurations, examples
           - Level 4: Advanced topics, edge cases, troubleshooting
        
        2. **When to create child sections**:
           - When a topic has 3+ distinct subtopics
           - When separating overview from implementation details
           - When organizing related but independent features
           - When providing both conceptual and practical content
        
        3. **Comprehensive Coverage**:
           - Start with project overview and architecture
           - Document each major module/service separately
           - Include API documentation for each endpoint group
           - Add configuration and deployment sections
           - Include development guides and best practices
           - Add troubleshooting and FAQ sections
        
        ### Content Distribution Strategy
        - **Parent nodes**: High-level overviews, architectural decisions, relationships
        - **Child nodes**: Specific implementations, code examples, detailed configurations
        - **Leaf nodes**: API references, parameter lists, error codes, examples
        
        ## REQUIRED DOCUMENTATION SECTIONS
        
        1. **项目概述** (Project Overview)
           - 项目简介
           - 快速开始
           - 系统需求
           - 项目结构说明
        
        2. **系统架构** (System Architecture)
           - 总体架构设计
           - 技术栈说明
           - 核心模块划分
             - 各模块详细设计
             - 模块间交互关系
           - 数据流设计
           - 部署架构
        
        3. **功能模块** (Feature Modules)
           - For each major feature/module:
             - 功能概述
             - 实现原理
             - 核心代码解析
             - 配置说明
             - 使用示例
             - 常见问题
        
        4. **API文档** (API Documentation)
           - API概览
           - For each API group:
             - 接口列表
             - 详细接口文档
               - 请求参数
               - 响应格式
               - 错误码说明
               - 调用示例
        
        5. **数据模型** (Data Models)
           - 数据库设计
           - 实体关系图
           - For each major entity:
             - 字段说明
             - 关联关系
             - 索引设计
        
        6. **配置指南** (Configuration Guide)
           - 环境配置
           - 应用配置
           - 性能调优
           - 安全配置
        
        7. **部署运维** (Deployment & Operations)
           - 部署方案
           - 监控方案
           - 日志管理
           - 故障处理
        
        8. **开发指南** (Development Guide)
           - 开发环境搭建
           - 编码规范
           - 测试指南
           - 贡献指南
        
        ## OUTPUT REQUIREMENTS
        
        <important>
        1. Generate a **multi-level hierarchical structure** with appropriate depth
        2. Each parent section should have **relevant child sections**
        3. Use **descriptive Chinese names** that clearly indicate content
        4. Include **specific file dependencies** for each section
        5. Write **detailed prompts** that request code examples and practical content
        6. Return ONLY valid JSON without any additional text
        </important>
        
        {
            "items": [
                {
                    "title": "project-overview",
                    "name": "项目概述",
                    "dependent_file": ["README.md", "package.json", "pom.xml"],
                    "prompt": "生成项目概述文档。包括：1) 项目的背景、目标和主要功能 2) 技术特点和优势 3) 适用场景和目标用户。请基于README和配置文件提供准确信息。",
                    "children": [
                        {
                            "title": "quick-start",
                            "name": "快速开始",
                            "dependent_file": ["README.md", "docker-compose.yml"],
                            "prompt": "创建快速开始指南。包括：1) 环境要求的详细列表 2) 安装步骤的具体命令 3) 首次运行的配置示例 4) 验证安装成功的方法。提供完整的命令行示例。",
                            "children": [
                                {
                                    "title": "installation-guide",
                                    "name": "安装指南",
                                    "dependent_file": ["package.json", "requirements.txt", "Dockerfile"],
                                    "prompt": "提供详细的安装指南。列出所有依赖项及其版本要求，提供不同操作系统下的安装命令，包括常见安装问题的解决方案。"
                                },
                                {
                                    "title": "first-demo",
                                    "name": "第一个示例",
                                    "dependent_file": ["examples/", "demo/"],
                                    "prompt": "创建一个简单但完整的使用示例。包括：1) 示例代码with详细注释 2) 运行步骤 3) 预期输出 4) 代码解释。确保初学者能够理解。"
                                }
                            ]
                        }
                    ]
                }
                // ... more sections following similar pattern
            ]
        }
        """;
    
    public static String promptV4 = """
        You are an expert technical documentation specialist with advanced software development knowledge. Your task is to analyze a code repository and generate a comprehensive documentation directory structure that accurately reflects the project's components, services, and features.
        
        First, review the following information about the repository:
        
        <code_files>
        {{$code_files}}
        </code_files>
        
        <repository_location>
        {{$repository_location}}
        </repository_location>
        
        Your goal is to create a documentation structure specifically tailored to this project, based on careful analysis of the provided code, README, and other project materials. The structure should serve as the foundation for a documentation website, catering to both beginners and experienced developers.
        
        Process:
        1. Create a hierarchical documentation structure that reflects the project's organization.
        2. Ensure the structure meets all the requirements listed below.
        3. Generate the final output in the specified JSON format.
        
        Requirements for the documentation structure:
        1. Include only sections that correspond to actual components, services, and features in the project.
        2. Use terminology consistent with the project code.
        3. Mirror the logical organization of the project in the structure.
        4. Cover every significant aspect of the project without omission.
        5. Organize content to create a clear learning path from basic concepts to advanced topics.
        6. Balance high-level overviews with detailed reference documentation.
        7. Include sections for getting started, installation, and basic usage.
        8. Provide dedicated sections for each major feature and service.
        9. Include API documentation sections for all public interfaces.
        10. Address configuration, customization, and extension points.
        11. Include troubleshooting and advanced usage sections where appropriate.
        12. Organize reference material in a logical, accessible manner.
        13. For each section, identify and include the most relevant source files from the project as dependent_file entries.
        14. Create deep hierarchical structures with multiple levels where appropriate.
        15. Don't hold back. Give it your all.
        
        Output format requirements:
        - Return ONLY a valid JSON object in the following exact format
        - Do not include any additional text, explanations, or markdown formatting
        - The `name` field must be in Chinese ("中文")
        - Ensure the JSON is properly formatted and can be parsed directly
        - Include dependent_file arrays with actual file paths from the repository
        - Create comprehensive prompt descriptions for each section
        
        {
            "items": [
                {
                    "title": "section-identifier",
                    "name": "Section Name (in Chinese)",
                    "dependent_file": [
                        "path/to/relevant/file1.ext",
                        "path/to/relevant/file2.ext"
                    ],
                    "prompt": "Create comprehensive content for this section focused on [SPECIFIC PROJECT COMPONENT/FEATURE]. Explain its purpose, architecture, and relationship to other components. Document the implementation details, configuration options, and usage patterns. Include both conceptual overviews for beginners and technical details for experienced developers. Use terminology consistent with the codebase. Provide practical examples demonstrating common use cases. Document public interfaces, parameters, and return values. Include diagrams where appropriate to illustrate key concepts.",
                    "children": [
                        {
                            "title": "subsection-identifier",
                            "name": "Subsection Name (in Chinese)",
                            "dependent_file": [
                                "path/to/relevant/subfile1.ext",
                                "path/to/relevant/subfile2.ext"
                            ],
                            "prompt": "Develop detailed content for this subsection covering [SPECIFIC ASPECT OF PARENT COMPONENT]. Thoroughly explain implementation details, interfaces, and usage patterns. Include concrete examples from the actual codebase. Document configuration options, parameters, and return values. Explain relationships with other components. Address common issues and their solutions. Make content accessible to beginners while providing sufficient technical depth for experienced developers."
                        }
                    ]
                }
            ]
        }
        """;
    
    // v5: 优化版本，减少文档碎片化，控制文档数量在8-15个
    public static String promptV5 = """
        You are a technical documentation architect focused on creating well-structured, consolidated documentation that avoids over-fragmentation.

        <code_files>
        {{$code_files}}
        </code_files>
        
        <repository_location>
        {{$repository_location}}
        </repository_location>

        ## CORE PRINCIPLES FOR DOCUMENTATION STRUCTURE

        ### 1. CONSOLIDATION OVER FRAGMENTATION
        - **Merge related components** into single documents (e.g., all entities, all DTOs, all utilities)
        - **Group simple classes** together rather than creating individual documents
        - **Create children only when necessary** - when a topic has substantial, distinct content
        - **Target 8-15 total documents** for typical projects, not 30+

        ### 2. WHEN TO CREATE CHILDREN
        Only create child sections when:
        - Parent topic has 3+ major distinct aspects requiring detailed explanation
        - Individual components have significant business logic (500+ lines of code)
        - Features have complex workflows that need separate documentation
        - API groups have 5+ endpoints requiring detailed documentation

        ### 3. WHEN TO CONSOLIDATE
        Always consolidate these into single documents:
        - Simple entity classes, DTOs, and value objects
        - Utility classes and helper functions
        - Configuration classes without complex logic
        - Similar service implementations
        - Small controller classes with basic CRUD operations

        ## DOCUMENTATION STRUCTURE STRATEGY

        ### Level 1: Major Areas (4-6 sections max)
        - **项目概述** - Overview, architecture, tech stack
        - **核心功能模块** - Main business features (consolidated)
        - **数据模型与持久化** - All entities, repositories, database design
        - **API接口文档** - All REST endpoints (grouped by domain)
        - **配置与部署** - Configuration, deployment, operations
        - **开发指南** - Development setup, testing, contributing

        ### Level 2: Consolidate Related Components
        - **实体与数据模型** - All entities in one document
        - **服务层实现** - Related services grouped together
        - **控制器接口** - REST controllers by functional area
        - **工具与组件** - Utilities, helpers, common components

        ### Level 3: Only for Complex Features
        Create children only for:
        - Complex business workflows with multiple steps
        - Large API groups (10+ endpoints)
        - Advanced configuration requiring detailed explanation

        ## OUTPUT REQUIREMENTS

        <important>
        1. **Generate 8-15 total documents maximum** for typical projects
        2. **Consolidate simple classes** - don't create separate docs for basic entities/DTOs
        3. **Use descriptive Chinese names** that indicate grouped content
        4. **Only create children** when parent topic requires 1500+ words of content
        5. **Group related files** in dependent_file arrays
        6. Return ONLY valid JSON without additional text
        7. The `name` field must be in Chinese ("中文")
        </important>

        {
            "items": [
                {
                    "title": "section-identifier", 
                    "name": "Section Name (in Chinese)",
                    "dependent_file": ["path/to/file1.ext", "path/to/file2.ext"],
                    "prompt": "Create consolidated documentation covering [GROUPED COMPONENTS]. Include all related classes, their purposes, key methods, and usage patterns. Group similar functionality together rather than separating into individual documents.",
                    "children": [
                        {
                            "title": "subsection-identifier",
                            "name": "Subsection Name (in Chinese)", 
                            "dependent_file": ["path/to/complex/component.ext"],
                            "prompt": "Document this complex component that requires detailed explanation due to its significant business logic and multiple integration points."
                        }
                    ]
                }
            ]
        }
        """;
}
