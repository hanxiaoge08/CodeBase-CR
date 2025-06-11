package com.hxg.crApp.service.port;

/**
 * 知识库服务接口
 * 
 * 定义知识库的生命周期管理
 */
public interface IKnowledgeBaseService {

    /**
     * 构建规范知识库
     * 加载《阿里巴巴Java开发手册》等文档
     */
    void buildStyleGuideKnowledgeBase();

    /**
     * 构建或更新项目知识库
     * 
     * @param repoFullName 仓库全名（如：owner/repo）
     */
    void buildOrUpdateProjectKnowledgeBase(String repoFullName);

    /**
     * 检查规范知识库是否已构建
     * 
     * @return true如果已构建
     */
    boolean isStyleGuideKnowledgeBaseBuilt();

    /**
     * 检查项目知识库是否已构建
     * 
     * @param repoFullName 仓库全名
     * @return true如果已构建
     */
    boolean isProjectKnowledgeBaseBuilt(String repoFullName);
} 