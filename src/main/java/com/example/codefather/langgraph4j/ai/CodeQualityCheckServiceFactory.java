package com.example.codefather.langgraph4j.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {

    @Resource(name = "chatModelPrototype")
    private ChatModel chatModel;

    /**
     * 创建代码质量检查服务实例
     */
    @Bean
    public CodeQualityCheckService codeQualityCheckService() {
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel)
                .build();
    }
}
