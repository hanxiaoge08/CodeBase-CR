package com.way.service;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;



/**
 * 聊天记忆服务
 * 基于taskId和userId管理聊天记忆，每个用户在每个任务下都有独立的对话历史
 * 
 * @author way
 */
@Service
public class ChatMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryService.class);
    private static final int DEFAULT_MAX_MESSAGES = 20; // 默认保留最近20条消息
    
    @Autowired
    private SQLiteChatMemoryRepository chatMemoryRepository;
    
    /**
     * 获取指定任务和用户的聊天记忆
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return ChatMemory实例
     */
    public ChatMemory getChatMemory(String taskId, String userId) {
        return getChatMemory(taskId, userId, DEFAULT_MAX_MESSAGES);
    }
    
    /**
     * 获取指定任务和用户的聊天记忆
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param maxMessages 最大消息数量
     * @return ChatMemory实例
     */
    public ChatMemory getChatMemory(String taskId, String userId, int maxMessages) {
        String conversationId = generateConversationId(taskId, userId);
        
        logger.debug("获取用户任务聊天记忆: taskId={}, userId={}, conversationId={}, maxMessages={}", 
                    taskId, userId, conversationId, maxMessages);
        
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }
    
    /**
     * 获取指定任务的聊天记忆（兼容旧接口，使用默认用户）
     * @param taskId 任务ID
     * @return ChatMemory实例
     * @deprecated 建议使用 getChatMemory(taskId, userId)
     */
    @Deprecated
    public ChatMemory getChatMemory(String taskId) {
        return getChatMemory(taskId, "default_user", DEFAULT_MAX_MESSAGES);
    }
    
    /**
     * 获取指定任务的聊天记忆（兼容旧接口，使用默认用户）
     * @param taskId 任务ID
     * @param maxMessages 最大消息数量
     * @return ChatMemory实例
     * @deprecated 建议使用 getChatMemory(taskId, userId, maxMessages)
     */
    @Deprecated
    public ChatMemory getChatMemory(String taskId, int maxMessages) {
        return getChatMemory(taskId, "default_user", maxMessages);
    }
    
    /**
     * 添加消息到指定用户和任务的聊天记忆
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param messages 消息列表
     */
    public void addMessages(String taskId, String userId, List<Message> messages) {
        try {
            ChatMemory chatMemory = getChatMemory(taskId, userId);
            String conversationId = generateConversationId(taskId, userId);
            
            for (Message message : messages) {
                chatMemory.add(conversationId, message);
            }
            
            logger.info("已添加{}条消息到用户任务记忆: taskId={}, userId={}", messages.size(), taskId, userId);
        } catch (Exception e) {
            logger.error("添加消息到用户任务记忆失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定用户和任务的聊天历史
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 聊天历史消息列表
     */
    public List<Message> getChatHistory(String taskId, String userId) {
        return getChatHistory(taskId, userId, DEFAULT_MAX_MESSAGES);
    }
    
    /**
     * 获取指定用户和任务的聊天历史
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param maxMessages 最大消息数量
     * @return 聊天历史消息列表
     */
    public List<Message> getChatHistory(String taskId, String userId, int maxMessages) {
        try {
            ChatMemory chatMemory = getChatMemory(taskId, userId, maxMessages);
            String conversationId = generateConversationId(taskId, userId);
            List<Message> history = chatMemory.get(conversationId);
            
            logger.debug("获取用户任务聊天历史: taskId={}, userId={}, 消息数量={}", taskId, userId, history.size());
            return history;
        } catch (Exception e) {
            logger.error("获取用户任务聊天历史失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 清除指定用户和任务的聊天记忆
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void clearChatMemory(String taskId, String userId) {
        try {
            ChatMemory chatMemory = getChatMemory(taskId, userId);
            String conversationId = generateConversationId(taskId, userId);
            chatMemory.clear(conversationId);
            
            logger.info("已清除用户任务聊天记忆: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            logger.error("清除用户任务聊天记忆失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查指定用户和任务是否有聊天历史
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否有聊天历史
     */
    public boolean hasChatHistory(String taskId, String userId) {
        try {
            List<Message> history = getChatHistory(taskId, userId, 1);
            return !history.isEmpty();
        } catch (Exception e) {
            logger.error("检查用户任务聊天历史失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
            return false;
        }
    }
    
    // ===== 兼容旧接口的方法 =====
    
    /**
     * 添加消息到指定任务的聊天记忆（兼容旧接口）
     * @param taskId 任务ID
     * @param messages 消息列表
     * @deprecated 建议使用 addMessages(taskId, userId, messages)
     */
    @Deprecated
    public void addMessages(String taskId, List<Message> messages) {
        addMessages(taskId, "default_user", messages);
    }
    
    /**
     * 获取指定任务的聊天历史（兼容旧接口）
     * @param taskId 任务ID
     * @return 聊天历史消息列表
     * @deprecated 建议使用 getChatHistory(taskId, userId)
     */
    @Deprecated
    public List<Message> getChatHistory(String taskId) {
        return getChatHistory(taskId, "default_user", DEFAULT_MAX_MESSAGES);
    }
    
    /**
     * 获取指定任务的聊天历史（兼容旧接口）
     * @param taskId 任务ID
     * @param maxMessages 最大消息数量
     * @return 聊天历史消息列表
     * @deprecated 建议使用 getChatHistory(taskId, userId, maxMessages)
     */
    @Deprecated
    public List<Message> getChatHistory(String taskId, int maxMessages) {
        return getChatHistory(taskId, "default_user", maxMessages);
    }
    
    /**
     * 清除指定任务的聊天记忆（兼容旧接口）
     * @param taskId 任务ID
     * @deprecated 建议使用 clearChatMemory(taskId, userId)
     */
    @Deprecated
    public void clearChatMemory(String taskId) {
        clearChatMemory(taskId, "default_user");
    }
    
    /**
     * 检查指定任务是否有聊天历史（兼容旧接口）
     * @param taskId 任务ID
     * @return 是否有聊天历史
     * @deprecated 建议使用 hasChatHistory(taskId, userId)
     */
    @Deprecated
    public boolean hasChatHistory(String taskId) {
        return hasChatHistory(taskId, "default_user");
    }
    
    /**
     * 生成会话ID
     * 基于taskId和userId生成唯一的会话标识符
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 会话ID
     */
    private String generateConversationId(String taskId, String userId) {
        if (!StringUtils.hasText(taskId)) {
            // 如果没有taskId，使用用户通用会话ID
            return "general_" + (StringUtils.hasText(userId) ? userId : "anonymous");
        }
        
        if (!StringUtils.hasText(userId)) {
            // 如果没有userId，使用默认用户
            userId = "default_user";
        }
        
        // 使用task_user_前缀确保用户和任务双重隔离
        return "task_" + taskId + "_user_" + userId;
    }
    
    /**
     * 添加纯净的对话记录（只包含用户问题和AI回答，不包含RAG上下文）
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param userQuery 用户问题
     * @param aiResponse AI回答
     */
    public void addCleanConversation(String taskId, String userId, String userQuery, String aiResponse) {
        try {
            ChatMemory chatMemory = getChatMemory(taskId, userId);
            String conversationId = generateConversationId(taskId, userId);
            
            // 只存储纯净的用户问题和AI回答
            UserMessage userMessage = new UserMessage(userQuery);
            AssistantMessage assistantMessage = new AssistantMessage(aiResponse);
            
            chatMemory.add(conversationId, userMessage);
            chatMemory.add(conversationId, assistantMessage);
            
            logger.info("已添加纯净对话记录到用户任务记忆: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            logger.error("添加纯净对话记录失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户任务聊天记忆的统计信息
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 统计信息字符串
     */
    public String getChatMemoryStats(String taskId, String userId) {
        try {
            List<Message> history = getChatHistory(taskId, userId);
            
            long userMessages = history.stream()
                    .filter(msg -> "user".equals(msg.getMessageType().getValue()))
                    .count();
            
            long assistantMessages = history.stream()
                    .filter(msg -> "assistant".equals(msg.getMessageType().getValue()))
                    .count();
            
            return String.format("用户 %s 在任务 %s 的聊天统计: 总消息 %d 条, 用户消息 %d 条, AI回复 %d 条", 
                                userId, taskId, history.size(), userMessages, assistantMessages);
                                
        } catch (Exception e) {
            logger.error("获取用户任务聊天统计失败: taskId={}, userId={}, error={}", taskId, userId, e.getMessage(), e);
            return "获取统计信息失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取任务聊天记忆的统计信息（兼容旧接口）
     * @param taskId 任务ID
     * @return 统计信息字符串
     * @deprecated 建议使用 getChatMemoryStats(taskId, userId)
     */
    @Deprecated
    public String getChatMemoryStats(String taskId) {
        return getChatMemoryStats(taskId, "default_user");
    }
}
