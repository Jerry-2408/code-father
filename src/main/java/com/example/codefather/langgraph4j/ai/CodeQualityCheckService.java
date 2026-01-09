package com.example.codefather.langgraph4j.ai;

import com.example.codefather.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码生成AI质检服务
 */
public interface CodeQualityCheckService {

    /**
     * 质检代码
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String codeContent);

}
