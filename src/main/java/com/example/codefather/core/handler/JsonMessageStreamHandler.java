package com.example.codefather.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.codefather.ai.message.*;
import com.example.codefather.ai.tools.BaseTool;
import com.example.codefather.ai.tools.ToolManager;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON消息流处理器
 * 处理VUE_PROJECT类型的复制流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private ToolManager toolManager;

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
                    // 特殊处理，异步构建Vue项目完成，通知前端
                    if ("__BUILD_DONE__".equals(chunk)) {
                        return "__BUILD_DONE__";
                    }
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
                // 获取TokenStream中封装的Json数据
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                // 解析数据
                String data = aiResponseMessage.getData();
                aiResponseBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                // 获取TokenStream中封装的Json数据
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                // 解析数据
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否第一次出现该工具ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次出现
                    seenToolIds.add(toolId);
                    // 根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 返回格式化的工具调用信息
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次出现
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 获取TokenStream中封装的Json数据
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                // 解析数据
                String toolName = toolExecutedMessage.getName();
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 根据工具名称获取工具实例
                BaseTool tool = toolManager.getTool(toolName);
                // 返回格式化的工具执行信息，并加入信息到对话历史中
                String result = tool.generateToolExecuteResult(jsonObject);
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
