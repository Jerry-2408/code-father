package com.example.codefather;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class}) // 不需要RedisEmbeddingStore的自动装配
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.example.codefather.mapper")
@EnableCaching
public class CodeFatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeFatherApplication.class, args);
    }

}
