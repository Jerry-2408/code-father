package com.example.codefather.langgraph4j.ai;

import com.example.codefather.langgraph4j.tools.ImageSearchTool;
import com.example.codefather.langgraph4j.tools.LogoGeneratorTool;
import com.example.codefather.langgraph4j.tools.MermaidDiagramTool;
import com.example.codefather.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource(name = "chatModelPrototype")
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    /**
     * 创建图片收集AI服务
     * @return
     */
    @Bean
    public ImageCollectionService imageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(imageSearchTool, logoGeneratorTool, undrawIllustrationTool, mermaidDiagramTool)
                .build();
    }

}
