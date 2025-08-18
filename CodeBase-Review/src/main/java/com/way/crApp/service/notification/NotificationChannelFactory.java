package com.way.crApp.service.notification;

import com.way.crApp.config.NotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知渠道工厂类
 * 支持动态注册和管理通知渠道，提供良好的可扩展性
 */
@Component
public class NotificationChannelFactory {

    private static final Logger logger = LoggerFactory.getLogger(NotificationChannelFactory.class);

    private final NotificationConfig notificationConfig;
    private final Map<String, NotificationChannel> channelMap;

    public NotificationChannelFactory(NotificationConfig notificationConfig,
                                    List<NotificationChannel> channels) {
        this.notificationConfig = notificationConfig;
        this.channelMap = channels.stream()
            .collect(Collectors.toMap(
                NotificationChannel::getChannelName,
                Function.identity()
            ));
        
        logger.info("通知渠道工厂初始化完成，已注册渠道: {}", channelMap.keySet());
    }

    /**
     * 获取所有启用的通知渠道
     *
     * @return 启用的通知渠道列表
     */
    public List<NotificationChannel> getEnabledChannels() {
        if (!notificationConfig.isEnabled()) {
            logger.debug("通知功能已全局禁用");
            return List.of();
        }

        return notificationConfig.getChannels().stream()
            .map(channelMap::get)
            .filter(java.util.Objects::nonNull)
            .filter(NotificationChannel::isEnabled)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定名称的通知渠道
     *
     * @param channelName 渠道名称
     * @return 通知渠道，如果不存在则返回null
     */
    public NotificationChannel getChannel(String channelName) {
        return channelMap.get(channelName);
    }

    /**
     * 检查指定渠道是否可用
     *
     * @param channelName 渠道名称
     * @return 是否可用
     */
    public boolean isChannelAvailable(String channelName) {
        NotificationChannel channel = channelMap.get(channelName);
        return channel != null && channel.isEnabled();
    }

    /**
     * 获取所有已注册的渠道名称
     *
     * @return 渠道名称列表
     */
    public List<String> getRegisteredChannelNames() {
        return List.copyOf(channelMap.keySet());
    }
}
