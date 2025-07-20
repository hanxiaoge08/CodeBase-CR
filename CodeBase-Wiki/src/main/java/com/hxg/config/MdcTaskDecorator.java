package com.hxg.config;

import cn.hutool.core.lang.UUID;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author hxg
 * @description: MDC线程装饰器
 * @date 2025/7/20 15:35
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {


    @Override
    public Runnable decorate(Runnable runnable) {
        return wrap(runnable);
    }

    public static Runnable wrap(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        final Map<String, String> previous = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if(previous!=null&&previous.containsKey("traceId")){
                    MDC.setContextMap(previous);
                }else {
                    MDC.put("traceId", UUID.fastUUID().toString(true));
                }
                runnable.run();
            } finally {
                MDC.remove("traceId");
            }
        };
    }
}
