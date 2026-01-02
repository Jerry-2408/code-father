package com.example.codefather.core.handler;

import cn.hutool.core.util.StrUtil;
import com.example.codefather.core.parser.CodeParserExecutor;
import com.example.codefather.core.saver.CodeFileSaverExecutor;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.service.ChatHistoryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 简单文本流处理器
 * 处理HTML和MULTI_FILE类型的的流式响应
 */
@Component
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML和MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux 原始流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 流式结果
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId,
                               User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                // map要返回流式结果，doOnNext不用返回流式结果
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式处理完成，保存AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                })
                .doOnError(error -> {
                    // 流式处理出错，保存出错消息到对话历史
                    String errorMessage = "AI回复失败" + error.getMessage();
                    if (StrUtil.isNotBlank(errorMessage)) {
                        chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                });
    }
}
