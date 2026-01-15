package com.example.codefather.service;

import com.mybatisflex.core.service.IService;
import com.example.codefather.model.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * 恢复对话记忆（包含工具调用信息）
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     * 添加原始对话消息
     *
     * @param appId 应用Id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户Id
     * @return 是否添加成功
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 获取原始对话消息对象
     * @param appId 应用Id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户Id
     * @return 对话消息对象
     */
    ChatHistoryOriginal getChatHistoryOriginal(Long appId, String message, String messageType, Long userId);

    /**
     * 批量添加原始对话消息
     *
     * @param chatHistoryOriginalList 对话消息列表
     * @return 是否添加成功
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据appId删除对话历史
     *
     * @param appId 应用Id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据appIds批量删除对话历史
     *
     * @param appIds 应用Id列表
     * @return 是否删除成功
     */
    boolean deleteByAppIds(List<Long> appIds);

    /**
     * 根据appId加载原始对话历史到记忆窗口缓存中
     *
     * @param appId 应用Id
     * @param chatMemory 会话记忆窗口
     * @param maxCount 最大消息数
     * @return 加载的会话数
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

}
