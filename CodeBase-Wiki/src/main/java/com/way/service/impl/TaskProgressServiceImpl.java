package com.way.service.impl;

import com.way.service.ITaskProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 任务进度管理服务实现
 * 基于Redis实现任务进度跟踪和幂等性控制
 */
@Service
@Slf4j
public class TaskProgressServiceImpl implements ITaskProgressService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis键前缀
    private static final String TASK_TOTAL_PREFIX = "task:%s:total";
    private static final String TASK_CONSUME_PREFIX = "task:%s:consume";
    private static final String IDEMPOTENT_PREFIX = "idempotent:catalogue:%s";

    // 幂等性Lua脚本
    private static final String IDEMPOTENT_LUA_SCRIPT = """
            local key = KEYS[1]
            local value = ARGV[1]
            local expire_time_ms = ARGV[2]
            return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
            """;

    private final DefaultRedisScript<String> idempotentScript;

    public TaskProgressServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.idempotentScript = new DefaultRedisScript<>();
        this.idempotentScript.setScriptText(IDEMPOTENT_LUA_SCRIPT);
        this.idempotentScript.setResultType(String.class);
    }

    @Override
    public void setTaskTotal(String taskId, long totalCount) {
        String key = String.format(TASK_TOTAL_PREFIX, taskId);
        redisTemplate.opsForValue().set(key, totalCount, 24, TimeUnit.HOURS);
        log.debug("设置任务总数: taskId={}, totalCount={}", taskId, totalCount);
    }

    @Override
    public long incrementConsumedCount(String taskId) {
        String key = String.format(TASK_CONSUME_PREFIX, taskId);
        Long result = redisTemplate.opsForValue().increment(key);
        
        if (result == null) {
            log.warn("增加消费计数返回null: taskId={}", taskId);
            return 0;
        }
        
        // 设置过期时间（如果是第一次设置）
        if (result == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        
        log.debug("增加消费计数: taskId={}, newCount={}", taskId, result);
        return result;
    }

    @Override
    public long getTaskTotal(String taskId) {
        String key = String.format(TASK_TOTAL_PREFIX, taskId);
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        return 0;
    }

    @Override
    public long getConsumedCount(String taskId) {
        String key = String.format(TASK_CONSUME_PREFIX, taskId);
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        return 0;
    }

    @Override
    public boolean isTaskCompleted(String taskId) {
        long total = getTaskTotal(taskId);
        long consumed = getConsumedCount(taskId);
        
        boolean completed = total > 0 && consumed >= total;
        log.debug("检查任务完成状态: taskId={}, total={}, consumed={}, completed={}", 
            taskId, total, consumed, completed);
        
        return completed;
    }

    @Override
    public void clearTaskProgress(String taskId) {
        String totalKey = String.format(TASK_TOTAL_PREFIX, taskId);
        String consumeKey = String.format(TASK_CONSUME_PREFIX, taskId);
        
        redisTemplate.delete(totalKey);
        redisTemplate.delete(consumeKey);
        
        log.info("清理任务进度数据: taskId={}", taskId);
    }

    @Override
    public IdempotentResult checkIdempotent(String catalogueId) {
        String key = String.format(IDEMPOTENT_PREFIX, catalogueId);
        
        try {
            // 执行Lua脚本进行幂等性检查
            // 参数：key=幂等唯一标识，value=0（消费中状态），expire_time_ms=过期时间
            String result = redisTemplate.execute(
                idempotentScript,
                Collections.singletonList(key),
                0,             // 设置值为0（处理中状态）
                600000L        // 10分钟过期时间
            );
            
            if (result == null) {
                // absentAndGet为空：代表消息是第一次到达，执行完LUA脚本后，在Redis设置Key的Value值为0，消费中状态
                log.debug("消息第一次到达: catalogueId={}", catalogueId);
                return IdempotentResult.FIRST_TIME;
            } else if ("0".equals(result)) {
                // absentAndGet为0：代表已经有相同消息到达并且还没有处理完，通过抛异常的形式让MQ重试
                log.warn("消息正在处理中，需要重试: catalogueId={}", catalogueId);
                return IdempotentResult.PROCESSING;
            } else if ("1".equals(result)) {
                // absentAndGet为1：代表已经有相同消息消费完成，返回空表示不执行任何处理
                log.debug("消息已处理完成，跳过: catalogueId={}", catalogueId);
                return IdempotentResult.COMPLETED;
            } else {
                // 其他状态，按第一次处理
                log.warn("未知的幂等状态: catalogueId={}, result={}", catalogueId, result);
                return IdempotentResult.FIRST_TIME;
            }
            
        } catch (Exception e) {
            log.error("幂等性检查失败: catalogueId={}", catalogueId, e);
            // 异常情况下允许处理，避免消息丢失
            return IdempotentResult.FIRST_TIME;
        }
    }

    @Override
    public void markMessageCompleted(String catalogueId) {
        String key = String.format(IDEMPOTENT_PREFIX, catalogueId);
        
        try {
            // 设置为已完成状态（数字1）
            redisTemplate.opsForValue().set(key, 1, 10, TimeUnit.MINUTES);
            log.debug("标记消息处理完成: catalogueId={}", catalogueId);
        } catch (Exception e) {
            log.error("标记消息完成失败: catalogueId={}", catalogueId, e);
        }
    }
}
