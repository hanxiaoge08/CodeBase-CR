package com.hxg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableAsync
@EnableWebMvc
@EnableFeignClients
@Slf4j
public class AiCodeRepositoryWikiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeRepositoryWikiApplication.class,args);
    }
}