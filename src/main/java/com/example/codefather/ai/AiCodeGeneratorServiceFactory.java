package com.example.codefather.ai;

import com.example.codefather.ai.tools.*;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import com.example.codefather.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，appId：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据appId获取AI服务（使用缓存），这个方法是为了兼容之前的代码
     * @param appId 应用Id
     * @return AI服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }


    /**
     * 根据appId获取AI服务（使用缓存）
     * @param appId 应用Id
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return AI服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return serviceCache.get(buildCacheKey(appId, codeGenTypeEnum), key -> createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    /**
     * 根据appId创建新的AI服务实例
     * @param appId 应用Id
     * @return AI服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        // 根据appId构建独立的会话记忆窗口
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库中加载历史对话到记忆窗口中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 构建AI服务
        return switch (codeGenTypeEnum) {
            // VUE项目生成使用推理模型+工具调用
            case  VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest, "Error: there is no tool called" + toolExecutionRequest.name()))
                    .build();
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                            .chatModel(chatModel)
                            .streamingChatModel(openAiStreamingChatModel)
                            .chatMemory(chatMemory)
                            .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 构建缓存键
     * @param appId 应用Id
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 缓存键
     */
    private String buildCacheKey(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return appId + "_" + codeGenTypeEnum.getValue();
    }

//    /**
//     * 默认提供一个AI服务Bean，兼容之前的代码
//     * @return
//     */
//    @Bean
//    public AiCodeGeneratorService aiCodeGeneratorService() {
//        return getAiCodeGeneratorService(0L);
//    }
}
