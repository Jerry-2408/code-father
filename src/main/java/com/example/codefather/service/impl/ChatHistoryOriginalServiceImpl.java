package com.example.codefather.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.example.codefather.ai.message.ToolExecutedMessage;
import com.example.codefather.ai.message.ToolRequestMessage;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.codefather.model.entity.ChatHistoryOriginal;
import com.example.codefather.mapper.ChatHistoryOriginalMapper;
import com.example.codefather.service.ChatHistoryOriginalService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@Slf4j
@Service
public class ChatHistoryOriginalServiceImpl extends ServiceImpl<ChatHistoryOriginalMapper, ChatHistoryOriginal>  implements ChatHistoryOriginalService{

    @Override
    public boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(message == null || message.length() == 0, ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(messageType == null || messageType.length() == 0, ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");
        // 对话消息入库
        ChatHistoryOriginal chatHistoryOriginal = ChatHistoryOriginal.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistoryOriginal);
    }

    @Override
    public ChatHistoryOriginal getChatHistoryOriginal(Long appId, String message, String messageType, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(message == null || message.length() == 0, ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(messageType == null || messageType.length() == 0, ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");

        return ChatHistoryOriginal.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
    }

    @Override
    public boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList) {
        // 参数校验
        ThrowUtils.throwIf(chatHistoryOriginalList.isEmpty(), ErrorCode.PARAMS_ERROR, "对话消息列表不能为空");
        // 过滤无效对话消息
        List<ChatHistoryOriginal> validChatHistoryOriginalList = chatHistoryOriginalList.stream()
                .filter(chatHistoryOriginal -> {
                    ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(chatHistoryOriginal.getMessageType());
                    if (messageTypeEnum == null) {
                        log.error("不支持的对话消息类型：{}", chatHistoryOriginal.getMessageType());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        // 如果没有有效的对话消息，则返回失败
        if (validChatHistoryOriginalList.isEmpty()) {
            return false;
        }
        // 批量入库
        return this.saveBatch(validChatHistoryOriginalList);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId);
        return this.remove(queryWrapper);
    }

    @Override
    public int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 1. 查找对话历史记录
            List<ChatHistoryOriginal> chatHistoryOriginalList = queryHistoryWithEdgeCheck(appId, maxCount);
            if (CollUtil.isEmpty(chatHistoryOriginalList)) {
                return 0;
            }
            // 2. 反转列表，老的在前，新的在后
            chatHistoryOriginalList = chatHistoryOriginalList.reversed();
            // 3. 先清理对话历史缓存，防止重复加载
            chatMemory.clear();
            // 4. 遍历对话历史记录，添加到记忆窗口中
            int loadedCount = loadMessagesToMemory(chatHistoryOriginalList, chatMemory);
            log.info("加载对话历史记录成功，appId：{}, loadedCount：{}", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载对话历史记录失败，appId：{}, error：{}", appId, e.getMessage(), e);
        }
        return 0;
    }

    private int loadMessagesToMemory(List<ChatHistoryOriginal> chatHistoryOriginalList, MessageWindowChatMemory chatMemory) {
        int loadedCount = 0;
        // 遍历对话历史记录，根据类型将消息添加到记忆中
        for (ChatHistoryOriginal chatHistoryOriginal : chatHistoryOriginalList) {
            String messageType = chatHistoryOriginal.getMessageType();
            ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
            switch (messageTypeEnum) {
                case USER -> {
                    chatMemory.add(UserMessage.from(chatHistoryOriginal.getMessage()));
                    loadedCount++;
                }
                case AI -> {
                    chatMemory.add(AiMessage.from(chatHistoryOriginal.getMessage()));
                    loadedCount++;
                }
                case TOOL_EXECUTION_REQUEST -> {
                    ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chatHistoryOriginal.getMessage(), ToolRequestMessage.class);
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .id(toolRequestMessage.getId())
                            .name(toolRequestMessage.getName())
                            .arguments(toolRequestMessage.getArguments())
                            .build();
                    // 有些工具调用请求带有文本，有些没有
                    if (toolRequestMessage.getText().isEmpty()) {
                        chatMemory.add(AiMessage.from(List.of(toolExecutionRequest)));
                    } else {
                        chatMemory.add(AiMessage.from(toolRequestMessage.getText(), List.of(toolExecutionRequest)));
                    }
                    loadedCount++;
                }
                case TOOL_EXECUTION_RESULT -> {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chatHistoryOriginal.getMessage(), ToolExecutedMessage.class);
                    chatMemory.add(ToolExecutionResultMessage.from(
                            toolExecutedMessage.getId(),
                            toolExecutedMessage.getName(),
                            toolExecutedMessage.getResult()
                    ));
                    loadedCount++;
                }
                default -> {
                    log.error("不支持的对话消息类型：{}", messageType);
                }
            }
        }
        return loadedCount;
    }

    /**
     * 查询对话历史记录（检查边界）
     * @param appId 应用 ID
     * @param maxCount 最大数量
     * @return 对话历史记录
     */
    private List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount) {
        // 1. 总对话历史记录数
        QueryWrapper countWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId);
        long totalCount = this.count(countWrapper);
        // 2. totalCount<=1，则返回空
        if (totalCount <= 1) {
            return List.of();
        }
        // 3. 计算实际可查询的最大数量
        long availableCount = totalCount - 1;
        // 4. 如果totalCount<=maxCount+1，则不需要检查边缘
        if (totalCount <= maxCount + 1) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistoryOriginal::getAppId, appId)
                    .orderBy(ChatHistoryOriginal::getId, false) // 根据ID倒序
                    .limit(1, maxCount);
            return this.list(queryWrapper);
        }
        // 5. 如果totalCount>maxCount+1，需要检查边缘
        // 5.1 查询第maxCount+1条记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false) // 根据ID倒序
                .limit(maxCount, 1); // 查询第 maxCount+1 条记录
        ChatHistoryOriginal edgeChatHistory = this.getOne(queryWrapper);

        // 5.2 边缘为TOOL_EXECUTION_RESULT类型，则需要额外查询其前一条TOOL_EXECUTION_REQUEST记录
        boolean needExtra = false;
        if (edgeChatHistory != null) {
            String edgeMessageType = edgeChatHistory.getMessageType();
            needExtra = (ChatHistoryMessageTypeEnum.getEnumByValue(edgeMessageType) == ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT);
        }
        // 5.3 计算实际需要查询的记录数
        availableCount = Math.min(needExtra ? maxCount + 1 : maxCount, availableCount);
        // 5.4 查询实际需要的对话历史记录
        QueryWrapper queryWrapper1 = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(1, availableCount);
        return this.list(queryWrapper1);
    }
}
