package com.example.codefather.ai;

import com.example.codefather.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * Ai代码生成类型路由服务
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     * @param userPrompt 用户需求
     * @return 代码生成类型
     */
    @SystemMessage(fromResource = "prompt/code-gen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);
}
