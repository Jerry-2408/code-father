package com.example.codefather.langgraph4j.node;

import com.example.codefather.constant.AppConstant;
import com.example.codefather.core.AiCodeGeneratorFacade;
import com.example.codefather.langgraph4j.model.QualityResult;
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

            // 构造用户消息（包含增强的提示词或者可能的错误修复信息）
            String userMessage = buildUserMessage(context);
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

    /**
     * 构造用户消息，如果存在质检失败结果则添加错误修复信息
     */
    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        // 检查是否存在质检失败结果
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 直接将错误修复信息作为新的提示词（起到了修改的作用）
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误信息
        qualityResult.getErrors().forEach(error -> errorInfo.append("- ").append(error).append("\n"));
        // 添加修改建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修改建议：\n");
            qualityResult.getSuggestions().forEach(suggestion -> errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n 请根据以上错误信息和建议重新生成或修改代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

}
