package com.example.codefather.ai;

import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个程序员黄伟乐的博客，代码不超过50行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("做一个程序员黄伟乐的留言板，代码不超过50行");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateMultiFileCode("你刚刚做了什么");
        Assertions.assertNotNull(result);
    }
}