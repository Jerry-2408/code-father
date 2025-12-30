package com.example.codefather.ai;

import com.example.codefather.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，appId：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据appId获取AI服务（使用缓存）
     * @param appId 应用Id
     * @return AI服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 根据appId创建新的AI服务实例
     * @param appId 应用Id
     * @return AI服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId) {
        // 根据appId构建独立的会话记忆窗口
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载历史对话到记忆窗口中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 构建AI服务
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 默认提供一个AI服务Bean，兼容之前的代码
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}
