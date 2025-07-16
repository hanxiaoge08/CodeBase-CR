# 使用官方的OpenJDK 21镜像作为基础镜像
FROM openjdk:21-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven构建的jar文件
COPY target/cr-app-*.jar app.jar

# 创建知识库目录
RUN mkdir -p /app/knowledge-base

# 设置环境变量
ENV JAVA_OPTS="-Xmx1g -Xms512m"
ENV KNOWLEDGE_BASE_PATH="/app/knowledge-base"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/github/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 