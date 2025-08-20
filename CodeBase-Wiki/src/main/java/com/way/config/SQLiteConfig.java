package com.way.config;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Properties;

/**
 * @author way
 * @description: TODO
 * @date 2025/7/20 12:23
 */
@Configuration
public class SQLiteConfig {
    @Bean
    public SQLiteChatMemoryRepository sqliteChatMemoryRepository(){
        DriverManagerDataSource dataSource=new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:data/codebasewiki_db.sqlite");
        
        // 设置SQLite连接属性，包括时间格式
        Properties properties = new Properties();
        properties.setProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");
        dataSource.setConnectionProperties(properties);
        
        JdbcTemplate jdbcTemplate=new JdbcTemplate(dataSource);
        return SQLiteChatMemoryRepository.sqliteBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }
}
