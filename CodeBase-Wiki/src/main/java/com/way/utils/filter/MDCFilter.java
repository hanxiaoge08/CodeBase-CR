package com.way.utils.filter;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * @author way
 * @description: MDC过滤器
 * @date 2025/7/20 15:41
 */
@Slf4j
@WebFilter(urlPatterns = "/*",filterName = "MDCFilter")
public class MDCFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        boolean hasTraceId= StringUtils.isNotBlank(MDC.get("traceId"));
        try{
            if(!hasTraceId){
                String traceId = UUID.randomUUID().toString(true);
                MDC.put("traceId", traceId);
            }
            filterChain.doFilter(servletRequest, servletResponse);
        }finally {
            MDC.remove("traceId");
        }
    }
}
