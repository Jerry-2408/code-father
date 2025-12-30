package com.example.codefather.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.codefather.constant.UserConstant;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.model.dto.chatHistory.ChatHistoryQueryDTO;
import com.example.codefather.model.entity.App;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.service.AppService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.codefather.model.entity.ChatHistory;
import com.example.codefather.mapper.ChatHistoryMapper;
import com.example.codefather.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 构造查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount); // ?
            // 查询
            List<ChatHistory> historyList = this.list(queryWrapper);
            // 反转列表（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆窗口中
            int loadedCount = 0;
            // 先清理对话历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                // 添加用户消息到记忆窗口
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                    // 添加AI消息到记忆窗口
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            return loadedCount;
        } catch (Exception e) {
            log.error("加载对话历史失败：appId：{}，error：{}", appId, e.getMessage());
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");
        // 保存消息记录
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId);
        return this.remove(queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 校验权限，只用应用创建者和管理员可以查看对话历史
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(app.getUserId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()),
                        ErrorCode.NO_AUTH_ERROR,
                        "无权限查看应用的对话历史");
        // 查询
        ChatHistoryQueryDTO chatHistoryQueryDTO = new ChatHistoryQueryDTO();
        chatHistoryQueryDTO.setAppId(appId);
        chatHistoryQueryDTO.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryDTO);
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryDTO chatHistoryQueryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryDTO == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryDTO.getId();
        String message = chatHistoryQueryDTO.getMessage();
        String messageType = chatHistoryQueryDTO.getMessageType();
        Long appId = chatHistoryQueryDTO.getAppId();
        Long userId = chatHistoryQueryDTO.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryDTO.getLastCreateTime();
        String sortField = chatHistoryQueryDTO.getSortField();
        String sortOrder = chatHistoryQueryDTO.getSortOrder();
        queryWrapper.eq(ChatHistory::getId, id)
                .like(ChatHistory::getMessage, message)
                .eq(ChatHistory::getMessageType, messageType)
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getUserId, userId);
        if (lastCreateTime != null) {
            queryWrapper.lt(ChatHistory::getCreateTime, lastCreateTime);
        }
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy(ChatHistory::getCreateTime, false);
        }
        return queryWrapper;
    }
}
