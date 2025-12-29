package com.example.codefather.controller;

import com.example.codefather.annotation.AuthCheck;
import com.example.codefather.common.BaseResponse;
import com.example.codefather.common.ResultUtils;
import com.example.codefather.constant.UserConstant;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.model.dto.chatHistory.ChatHistoryQueryDTO;
import com.example.codefather.model.entity.User;
import com.example.codefather.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.codefather.model.entity.ChatHistory;
import com.example.codefather.service.ChatHistoryService;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询应用的对话历史（游标查询）
     * @param appId 应用 ID
     * @param pageSize 页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request 请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 查询
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     * @param chatHistoryQueryDTO 查询条件
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listChatHistoryByPage(@RequestBody ChatHistoryQueryDTO chatHistoryQueryDTO) {
        ThrowUtils.throwIf(chatHistoryQueryDTO == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryDTO.getPageNum();
        long pageSize = chatHistoryQueryDTO.getPageSize();
        // 查询
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryDTO);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(chatHistoryPage);
    }
}
