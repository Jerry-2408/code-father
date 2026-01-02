package com.example.codefather.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.codefather.ai.message.*;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON消息流处理器
 * 处理VUE_PROJECT类型的复制流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /**
     * 处理TokenStream流封装的消息（VUE_PROJECT）
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
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个JSON块
                    return handleJsonMessageChunk(chunk, aiResponseBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误信息
                    String errorMessage = "AI回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集在TokenStream中封装的Json数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder aiResponseBuilder, Set<String> seenToolIds) {
        // 解析JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                aiResponseBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否第一次出现该工具ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次出现
                    seenToolIds.add(toolId);
                    return "\n\n[选择工具] 写入文件\n\n";
                } else {
                    // 不是第一次出现
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                String result = String.format("""
                        [工具调用]写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                String output = String.format("\n\n%s\n\n", result);
                aiResponseBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型：{}", typeEnum);
                return "";
            }
        }
    }
}
