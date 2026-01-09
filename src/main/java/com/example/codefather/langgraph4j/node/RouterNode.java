package com.example.codefather.langgraph4j.node;

import com.example.codefather.ai.AiCodeGenTypeRoutingService;
import com.example.codefather.langgraph4j.state.WorkflowContext;
import com.example.codefather.langgraph4j.utils.SpringContextUtil;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");
            
            CodeGenTypeEnum codeGenType;
            try {
                // 获取AI智能路由服务
                AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                // 根据原始提示词进行智能路由
                codeGenType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("智能路由完成，选择类型: {}, {}", codeGenType.getText(), codeGenType.getValue());
            } catch (Exception e) {
                log.error("智能路由失败，使用默认HTML类型：{}", e.getMessage());
                codeGenType = CodeGenTypeEnum.HTML;
            }
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(codeGenType);
            return WorkflowContext.saveContext(context);
        });
    }
}
