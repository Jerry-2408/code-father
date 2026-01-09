package com.example.codefather.langgraph4j.node;

import com.example.codefather.constant.AppConstant;
import com.example.codefather.core.AiCodeGeneratorFacade;
import com.example.codefather.langgraph4j.state.WorkflowContext;
import com.example.codefather.langgraph4j.utils.SpringContextUtil;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CodeGeneratorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码生成");
            
            // 使用增强提示词作为发给AI地用户消息
            String userMessage = context.getEnhancedPrompt();
            CodeGenTypeEnum codeGenType= context.getGenerationType();
            // 获取AI代码生成门面服务实例
            AiCodeGeneratorFacade aiCodeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            // 生成代码
            log.info("开始生成代码，类型: {},{}", codeGenType.getText(), codeGenType.getValue());
            Long appId = 1L;
            Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenType, appId);
            // 同步等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(20));
            // 根据类型生成代码目录
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType.getValue(), appId);
            log.info("代码生成完成，生成目录: {}", generatedCodeDir);
            // 更新状态
            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
