package com.example.codefather.ai;

import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
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
     * @param userMessage 用户消息
     * @return HTML代码
     */
    @SystemMessage(fromResource = "prompt/code-gen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户消息
     * @return 多文件代码
     */
    @SystemMessage(fromResource = "prompt/code-gen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}
