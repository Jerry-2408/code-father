package com.example.codefather.core.handler;

import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import com.example.codefather.service.ChatHistoryOriginalService;
import com.example.codefather.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 执行流处理器，内部会处理流式响应，拼接对话并加入到对话历史
     *
     * @param originFlux 原始流
     * @param chatHistoryService 对话历史服务
     * @param chatHistoryOriginalService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @param codeGenType 代码生成类型
     * @return 流式结果
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  ChatHistoryOriginalService chatHistoryOriginalService,
                                  Long appId,
                                  User loginUser,
                                  CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> simpleTextStreamHandler.handle(originFlux, chatHistoryService, chatHistoryOriginalService, appId, loginUser);
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(originFlux, chatHistoryService, chatHistoryOriginalService, appId, loginUser);
        };
    }

}
