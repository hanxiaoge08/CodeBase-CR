package com.way.utils;

/**
 * @author way
 * @description: 任务id生成
 * @date 2025/7/23 16:57
 */
public class TaskIdGenerator {
    public static String generate() {
        return "TASK_" + System.currentTimeMillis();
    }

}
