package com.example.codefather.core;

import com.example.codefather.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("做一个程序员黄伟乐的博客，代码不超过50行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> chatResult = aiCodeGeneratorFacade.generateAndSaveCodeStream("做一个博客登录页面，代码不超过50行", CodeGenTypeEnum.MULTI_FILE);
        List<String> resultList = chatResult.collectList().block();
        Assertions.assertNotNull(resultList);
        String completeContent = String.join("", resultList);
        Assertions.assertNotNull(completeContent);
    }
}