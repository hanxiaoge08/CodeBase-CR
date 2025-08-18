# wiki模块
本项目是一个Spring ai alibaba的项目，es使用8.15.0版本，地址用户名为elastic 密码为password

es支持混合索引，导入的依赖为

```
 <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-elasticsearch</artifactId>
        </dependency>
    </dependencies>
```

es两个索引结构如下：

```
{
  "code_chunks_index": {
    "aliases": {},
    "mappings": {
      "dynamic": "false",
      "properties": {
        "apiName": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword"
            }
          },
          "analyzer": "code_ana"
        },
        "chunk_size": {
          "type": "integer"
        },
        "className": {
          "type": "keyword"
        },
        "content": {
          "type": "text",
          "term_vector": "with_positions_offsets",
          "analyzer": "code_ana"
        },
        "docSummary": {
          "type": "text",
          "analyzer": "zh_ik"
        },
        "language": {
          "type": "keyword"
        },
        "methodName": {
          "type": "keyword"
        },
        "mtime": {
          "type": "date"
        },
        "repoId": {
          "type": "keyword"
        },
        "sha256": {
          "type": "keyword"
        },
        "taskId": {
          "type": "keyword"
        },
        "vector": {
          "type": "dense_vector",
          "dims": 1024,
          "index": true,
          "similarity": "cosine",
          "index_options": {
            "type": "int8_hnsw",
            "m": 16,
            "ef_construction": 100
          }
        }
      }
    },
    "settings": {
      "index": {
        "routing": {
          "allocation": {
            "include": {
              "_tier_preference": "data_content"
            }
          }
        },
        "number_of_shards": "3",
        "provided_name": "code_chunks_index",
        "creation_date": "1755402591469",
        "analysis": {
          "analyzer": {
            "zh_ik": {
              "filter": [
                "lowercase"
              ],
              "type": "custom",
              "tokenizer": "ik_max_word"
            },
            "code_ana": {
              "filter": [
                "lowercase",
                "kstem"
              ],
              "type": "custom",
              "tokenizer": "pattern"
            }
          }
        },
        "number_of_replicas": "1",
        "uuid": "_Y8YSdx_TTiyWSkEK8l5iw",
        "version": {
          "created": "8512000"
        }
      }
    }
  }
}
```

```
{
  "documents_index": {
    "aliases": {},
    "mappings": {
      "dynamic": "false",
      "properties": {
        "catalogueId": {
          "type": "keyword"
        },
        "content": {
          "type": "text",
          "term_vector": "with_positions_offsets",
          "analyzer": "zh_ik"
        },
        "mtime": {
          "type": "date"
        },
        "name": {
          "type": "keyword"
        },
        "repoId": {
          "type": "keyword"
        },
        "sha256": {
          "type": "keyword"
        },
        "status": {
          "type": "integer"
        },
        "taskId": {
          "type": "keyword"
        },
        "vector": {
          "type": "dense_vector",
          "dims": 1024,
          "index": true,
          "similarity": "cosine",
          "index_options": {
            "type": "int8_hnsw",
            "m": 16,
            "ef_construction": 100
          }
        }
      }
    },
    "settings": {
      "index": {
        "routing": {
          "allocation": {
            "include": {
              "_tier_preference": "data_content"
            }
          }
        },
        "number_of_shards": "3",
        "provided_name": "documents_index",
        "creation_date": "1755402591690",
        "analysis": {
          "analyzer": {
            "zh_ik": {
              "filter": [
                "lowercase"
              ],
              "type": "custom",
              "tokenizer": "ik_max_word"
            }
          }
        },
        "number_of_replicas": "1",
        "uuid": "0XGdoZ4YSM6n3Em-bbMgjg",
        "version": {
          "created": "8512000"
        }
      }
    }
  }
}
```
目前正在做的事情是用es当做rag，不用原本的mem0了

# review模块
es kNN + rescore混合检索 
Spring AI 项目里，用本地rag Elasticsearch 实现混合检索，用kNN  + BM25 rescore 做检索，然后把检索结果交给大模型。

# 钉钉飞书通知
## 方案
wiki模块任务处理完进行飞书或钉钉通知，由于用了kafka消息队列，需要用redis保存进度，检查生产、消费的数量，
对生产及消费进行incr计数，当消费总数等于生产总数时，就可以认定消费已完成，只在业务处理成功并确认时才incr，同时需要用catalogueId做幂等。

redis结构

```
task:{taskId}:total      // Long，总任务数（生产端在发送前写入一次）
task:{taskId}:consume    // Long，已消费成功的任务数
```

consume的值incr原子自增，返回自增后的值进行判断是否等于consume，等于后进行消息推送，不会有并发问题。

kafka消费者幂等性

- `SET key value`: 在 Redis 中设置 `key` 对应的值为 `value`。
- `'NX'`: 表示只有在 `key` 不存在时才执行设置操作，防止覆盖已有值。
- `'GET'`: 表示在设置新值之前，获取并返回设置前的旧值（Redis 6.2 开始支持 `SET` 命令的 `GET` 选项）。
- `'PX expire_time_ms'`: 设置 `key` 的过期时间，单位是毫秒。

该脚本的主要作用是：在 Redis 中尝试以 `NX` 方式设置一个键，即如果键不存在，则设置新值，并返回设置之前的旧值，同时为该键设置过期时间（以毫秒为单位）。catalogueId做幂等key，过期时间设置为10分钟

获取到 Redis 里面的 Key 值后，可能会有三个流程执行：

- `absentAndGet` 为空：代表消息是第一次到达，执行完 LUA 脚本后，会在 Redis 设置 Key 的 Value 值为 0，消费中状态。
- `absentAndGet` 为 0：代表已经有相同消息到达并且还没有处理完，会通过抛异常的形式让 MQ 重试。
- `absentAndGet` 为 1：代表已经有相同消息消费完成，返回空表示不执行任何处理。

```
LUA_SCRIPT = """
            local key = KEYS[1]
            local value = ARGV[1]
            local expire_time_ms = ARGV[2]
            return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
            """;
           
```
review模块任务在Graph工作流的执行结果处理完之后就可以进行钉钉或飞书回调

redis的地址为：your-address:6379  密码为：your-password

## 飞书
### 文档RAG机器人
webhook：your-feishu-wiki-webhook
密钥：your-feishu-wiki-secret
### 代码评审机器人
your-feishu-review-webhook
密钥：your-feishu-review-secret

## 钉钉
### 文档RAG机器人
webhook：your-dingtalk-wiki-webhook
密钥：your-dingtalk-wiki-secret
### 代码评审机器人
webhook：your-dingtalk-review-webhook
密钥：your-dingtalk-review-secret