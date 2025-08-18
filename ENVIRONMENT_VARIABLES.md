# 环境变量配置文档

本文档描述了项目中需要配置的环境变量，这些变量替代了原先硬编码在配置文件中的敏感信息。

## 目录
- [通用配置](#通用配置)
- [CodeBase-Wiki 服务配置](#codebase-wiki-服务配置)
- [CodeBase-Review 服务配置](#codebase-review-服务配置)
- [部署说明](#部署说明)

## 通用配置

### Redis 配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `REDIS_HOST` | Redis 服务器地址 | `your-address` | ❌ |
| `REDIS_PORT` | Redis 服务器端口 | `6379` | ❌ |
| `REDIS_PASSWORD` | Redis 连接密码 | `your-redis-password` | ❌ |
| `REDIS_TIMEOUT` | Redis 连接超时时间 | `5000` | ❌ |

### 通知服务配置

#### 飞书机器人配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `FEISHU_ENABLED` | 是否启用飞书通知 | `true` | ❌ |
| `FEISHU_WIKI_WEBHOOK` | 飞书Wiki文档机器人webhook URL | 见配置文件 | ✅ |
| `FEISHU_WIKI_SECRET` | 飞书Wiki文档机器人密钥 | 见配置文件 | ✅ |
| `FEISHU_REVIEW_WEBHOOK` | 飞书代码评审机器人webhook URL | 见配置文件 | ✅ |
| `FEISHU_REVIEW_SECRET` | 飞书代码评审机器人密钥 | 见配置文件 | ✅ |

#### 钉钉机器人配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `DINGTALK_ENABLED` | 是否启用钉钉通知 | `true` | ❌ |
| `DINGTALK_WIKI_WEBHOOK` | 钉钉Wiki文档机器人webhook URL | 见配置文件 | ✅ |
| `DINGTALK_WIKI_SECRET` | 钉钉Wiki文档机器人密钥 | 见配置文件 | ✅ |
| `DINGTALK_REVIEW_WEBHOOK` | 钉钉代码评审机器人webhook URL | 见配置文件 | ✅ |
| `DINGTALK_REVIEW_SECRET` | 钉钉代码评审机器人密钥 | 见配置文件 | ✅ |

## CodeBase-Wiki 服务配置

### 基础配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `DASHSCOPE_API_KEY` | 阿里云大模型API密钥 | 无 | ✅ |

### Kafka 配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 集群地址 | `your-address:9092` | ❌ |

### Elasticsearch 配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `ELASTICSEARCH_URIS` | Elasticsearch 集群地址 | `http://your-address:9200` | ❌ |
| `ELASTICSEARCH_USERNAME` | Elasticsearch 用户名 | `elastic` | ❌ |
| `ELASTICSEARCH_PASSWORD` | Elasticsearch 密码 | `your-elasticsearch-password` | ❌ |

### 代码解析服务配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `CODE_PARSER_URL` | 代码解析服务地址 | `http://your-address:8566` | ❌ |

### 项目配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `GIT_REPOSITORY_BASE_PATH` | Git仓库本地克隆根目录 | `D:\Code\repository` | ❌ |

## CodeBase-Review 服务配置

### 基础配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `DASHSCOPE_API_KEY` | 阿里云大模型API密钥 | 见配置文件 | ❌ |

### GitHub 配置
| 环境变量 | 描述 | 默认值 | 必需 |
|---------|------|--------|------|
| `GITHUB_TOKEN` | GitHub 访问令牌 | 见配置文件 | ✅ |
| `GITHUB_WEBHOOK_SECRET` | GitHub Webhook 密钥 | `your-webhook-secret` | ❌ |

## 部署说明

### 1. 开发环境

在开发环境中，可以创建 `.env` 文件来设置环境变量：

```bash
# 创建 .env 文件
cp .env.example .env

# 编辑 .env 文件，设置实际的环境变量值
```

### 2. 生产环境

#### Docker 部署
在 `docker-compose.yml` 中设置环境变量：

```yaml
version: '3.8'
services:
  codebase-wiki:
    environment:
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      # ... 其他环境变量
```

#### Kubernetes 部署
使用 Secret 和 ConfigMap 来管理敏感信息：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
data:
  dashscope-api-key: <base64-encoded-value>
  github-token: <base64-encoded-value>
  # ... 其他敏感信息
```

#### 系统环境变量
在服务器上直接设置环境变量：

```bash
export DASHSCOPE_API_KEY="your-api-key"
export GITHUB_TOKEN="your-github-token"
# ... 其他环境变量
```

### 3. IDE 配置

#### IntelliJ IDEA
在运行配置中设置环境变量：
1. 打开 Run/Debug Configurations
2. 选择对应的应用程序配置
3. 在 Environment Variables 中添加所需的环境变量

#### VS Code
在 `.vscode/launch.json` 中配置：

```json
{
  "configurations": [
    {
      "name": "Spring Boot App",
      "env": {
        "DASHSCOPE_API_KEY": "your-api-key",
        "GITHUB_TOKEN": "your-github-token"
      }
    }
  ]
}
```

## 安全注意事项

1. **永远不要将敏感信息提交到版本控制系统**
2. **使用不同的密钥/令牌用于不同的环境**（开发、测试、生产）
3. **定期轮换API密钥和访问令牌**
4. **限制环境变量的访问权限**
5. **在生产环境中使用专门的密钥管理服务**（如 HashiCorp Vault、Azure Key Vault、AWS Secrets Manager）

## 故障排除

### 常见问题

1. **环境变量未生效**
   - 检查环境变量名称是否正确
   - 确保在应用启动前设置了环境变量
   - 重启应用程序以使环境变量生效

2. **默认值被使用**
   - 检查环境变量是否正确设置
   - 确认环境变量值不为空

3. **连接失败**
   - 验证网络连接和防火墙设置
   - 检查服务地址和端口是否正确
   - 确认认证信息（密码、令牌）是否有效

### 调试技巧

1. **查看当前环境变量**：
   ```bash
   # Linux/Mac
   env | grep -E "(REDIS|KAFKA|ELASTICSEARCH|GITHUB|DASHSCOPE|FEISHU|DINGTALK)"
   
   # Windows
   set | findstr /I "REDIS KAFKA ELASTICSEARCH GITHUB DASHSCOPE FEISHU DINGTALK"
   ```

2. **应用程序日志**：
   查看应用启动日志，确认配置是否正确加载

3. **配置验证**：
   在应用中添加配置验证逻辑，确保必需的环境变量都已设置

## 更新日志

- **2024-01-XX**: 初始版本，将硬编码配置迁移到环境变量
