package com.example.codefather.service;

import com.example.codefather.model.dto.chatHistory.ChatHistoryQueryDTO;
import com.example.codefather.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.example.codefather.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话消息记录
     * @param appId 应用id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户id
     * @return 添加结果
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 删除应用下的所有对话消息记录
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取应用下的对话消息记录
     * @param appId 应用id
     * @param pageSize 页面大小
     * @param lastCreateTime 应用对话的最后创建时间，用于分页查询，获取早于此时间的记录（游标查询，若用传统分页查询性能会很差）
     * @param loginUser 登录用户
     * @return 对话消息记录
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 获取查询包装器
     * @param chatHistoryQueryDTO 查询参数
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryDTO chatHistoryQueryDTO);
}
