package com.hxg.config;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author hxg
 * @description: TODO
 * @date 2025/7/20 12:23
 */
@Configuration
public class SQLiteConfig {
    @Bean
    public SQLiteChatMemoryRepository sqliteChatMemoryRepository(){
        DriverManagerDataSource dataSource=new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:data/chat-memory.db");
        JdbcTemplate jdbcTemplate=new JdbcTemplate(dataSource);
        return SQLiteChatMemoryRepository.sqliteBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }
}
