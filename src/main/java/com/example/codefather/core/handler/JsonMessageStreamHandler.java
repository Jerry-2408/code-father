package com.example.codefather.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.codefather.ai.message.*;
import com.example.codefather.ai.tools.BaseTool;
import com.example.codefather.ai.tools.ToolManager;
import com.example.codefather.model.entity.ChatHistoryOriginal;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.service.ChatHistoryOriginalService;
import com.example.codefather.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    @Autowired
    private ChatHistoryOriginalService chatHistoryOriginalService;

    /**
     * 处理TokenStream流封装的消息（VUE_PROJECT）
     * @param originFlux 原始流
     * @param chatHistoryService 对话历史服务
     * @param chatHistoryOriginalService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 流式结果
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               ChatHistoryOriginalService chatHistoryOriginalService,
                               Long appId,
                               User loginUser) {
        // 收集数据用于前端展示
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 收集用于恢复对话记忆的数据
        StringBuilder originalAiResponseBuilder = new StringBuilder();
        // 每个 Flux 流可能包含多条工具调用和 AI_RESPONSE 响应信息，统一收集之后批量入库
        List<ChatHistoryOriginal> originalChatHistoryList = new ArrayList<>();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 特殊处理，异步构建Vue项目完成，通知前端
                    if ("__BUILD_DONE__".equals(chunk)) {
                        return "__BUILD_DONE__";
                    }
                    // 解析每个JSON块
                    return handleJsonMessageChunk(appId, loginUser, chunk, aiResponseBuilder, originalAiResponseBuilder, originalChatHistoryList, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史（前端展示的）
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 流式响应完成后，添加AI消息到对话历史（后端缓存的）
                    String originalAiResponse = originalAiResponseBuilder.toString(); // 调用完所有工具后的AI消息或完全没调用工具的AI消息
                    originalChatHistoryList.add(chatHistoryOriginalService.getChatHistoryOriginal(appId, originalAiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId()));

                    originalChatHistoryList.forEach(chatHistoryOriginal -> {
                        chatHistoryOriginal.setAppId(appId);
                        chatHistoryOriginal.setUserId(loginUser.getId());
                    });
                    chatHistoryOriginalService.addOriginalChatMessageBatch(originalChatHistoryList);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误信息
                    String errorMessage = "AI回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    chatHistoryOriginalService.addOriginalChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集在TokenStream中封装的Json数据
     */
    private String handleJsonMessageChunk(Long appId,
                                          User loginUser,
                                          String chunk,
                                          StringBuilder aiResponseBuilder,
                                          StringBuilder originalAiResponseBuilder,
                                          List<ChatHistoryOriginal> originalChatHistoryList,
                                          Set<String> seenToolIds) {
        // 解析JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                // 获取TokenStream中封装的Json数据
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                // 解析数据
                String data = aiResponseMessage.getData();
                // 拼接数据
                aiResponseBuilder.append(data);
                originalAiResponseBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                // 调用工具前如果有AI消息的话，先将AI消息存入数据库
                if (originalAiResponseBuilder.length() > 0) {
                    String originalAiResponse = originalAiResponseBuilder.toString();
                    originalChatHistoryList.add(chatHistoryOriginalService.getChatHistoryOriginal(appId, originalAiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId()));
                    originalAiResponseBuilder.setLength(0);
                }
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
                    String requstString = tool.generateToolRequestResponse();
                    aiResponseBuilder.append(requstString);
                    return requstString;
                } else {
                    // 不是第一次出现
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 处理工具调用信息
                processToolExecutionMessage(originalAiResponseBuilder, chunk, originalChatHistoryList);
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

    /**
     * 解析处理工具调用相关信息
     * @param originalAiResponseBuilder
     * @param chunk
     * @param originalChatHistoryList
     */
    private void processToolExecutionMessage(StringBuilder originalAiResponseBuilder,
                                             String chunk,
                                             List<ChatHistoryOriginal> originalChatHistoryList) {
        // 解析 chunk
        ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
        // 构造工具调用请求对象(工具调用结果的数据就是从调用请求里拿的，所以直接在这里处理调用请求信息)
        String aiResponseStr = originalAiResponseBuilder.toString();
        ToolRequestMessage toolRequestMessage = new ToolRequestMessage();
        toolRequestMessage.setId(toolExecutedMessage.getId());
        toolRequestMessage.setName(toolExecutedMessage.getName());
        toolRequestMessage.setArguments(toolExecutedMessage.getArguments());
        toolRequestMessage.setText(aiResponseStr);
        // 转换成 JSON
        String toolRequestJsonStr = JSONUtil.toJsonStr(toolRequestMessage);
        // 构造 ChatHistory 存入列表
        ChatHistoryOriginal toolRequestHistory = ChatHistoryOriginal.builder()
                .message(toolRequestJsonStr)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_REQUEST.getValue())
                .build();
        originalChatHistoryList.add(toolRequestHistory);
        ChatHistoryOriginal toolResultHistory = ChatHistoryOriginal.builder()
                .message(chunk)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue())
                .build();
        originalChatHistoryList.add(toolResultHistory);
        // AI 响应内容暂时结束，置空 aiResponseStringBuilder
//        originalAiResponseBuilder.setLength(0);
    }

}
