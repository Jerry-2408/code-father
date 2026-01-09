package com.example.codefather.langgraph4j.node;

import com.example.codefather.core.builder.VueProjectBuilder;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.langgraph4j.state.WorkflowContext;
import com.example.codefather.langgraph4j.utils.SpringContextUtil;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");
            // 获取必要地参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum codeGenType = context.getGenerationType();
            String buildResultDir;
            // Vue类型项目，使用VueProjectBuilder进行构建
            try {
                // 获取VueProjectBuilder实例
                VueProjectBuilder vueProjectBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                // 构建Vue项目
                boolean buildResult = vueProjectBuilder.buildProject(generatedCodeDir);
                if (buildResult) {
                    // 构建成功，返回dist目录路径
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue项目构建成功，dist目录: {}", buildResultDir);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue项目构建失败");
                }
            } catch (Exception e) {
                log.error("Vue项目构建异常：{}", e.getMessage(), e);
                buildResultDir = generatedCodeDir; // 异常时返回原始代码目录
            }
            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建完成，结果目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
