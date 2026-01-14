package com.example.codefather.ai;

import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.*;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码
     *
     * @param userMessage 用户消息
     * @return HTML代码
     */
    @SystemMessage(fromResource = "prompt/code-gen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户消息
     * @return 多文件代码
     */
    @SystemMessage(fromResource = "prompt/code-gen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 流式生成HTML代码
     *
     * @param appId 应用Id
     * @param userMessage 用户消息
     * @return 流式结果
     */
    @SystemMessage(fromResource = "prompt/code-gen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(@MemoryId Long appId, @UserMessage String userMessage);

    /**
     * 流式生成多文件代码
     *
     * @param appId 应用Id
     * @param userMessage 用户消息
     * @return 流式结果
     */
    @SystemMessage(fromResource = "prompt/code-gen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@MemoryId Long appId, @UserMessage String userMessage);

    /**
     * 流式生成Vue项目代码
     *
     * @param appId 应用Id
     * @param userMessage 用户消息
     * @return 流式结果（TokenStream）
     */
    @SystemMessage(fromResource = "prompt/code-gen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId Long appId, @UserMessage String userMessage);
}
