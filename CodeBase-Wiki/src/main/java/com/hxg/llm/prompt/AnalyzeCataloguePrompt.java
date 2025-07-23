package com.hxg.llm.prompt;

/**
 * @author hxg
 * @description: 参考OpenDeepWiki的代码结构生成prompt
 * @date 2025/7/23 15:27
 */
public class AnalyzeCataloguePrompt {
    public static String prompt = """
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

}
