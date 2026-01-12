package com.example.codefather.ai;

import com.example.codefather.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    void routeCodeGenType() {
        String userPrompt = "做一个简单的个人介绍页面";
        CodeGenTypeEnum result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());
        userPrompt = "做一个公司官网，需要首页、关于我们、联系我们三个页面";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());
        userPrompt = "做一个电商管理系统，包含用户管理、商品管理、订单管理，需要路由和状态管理";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());
    }

    @Resource
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Test
    void testConcurrentRoutingCalls() throws InterruptedException {
        String[] prompts = {
                "做一个简单的HTML页面",
                "做一个多页面网站",
                "做一个vue管理系统"
        };
        // 使用虚拟线程并发执行
        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                AiCodeGenTypeRoutingService routingService = routingServiceFactory.createAiCodeGenTypeRoutingService();
                CodeGenTypeEnum codeGenTypeEnum = routingService.routeCodeGenType(prompt);
                log.info("线程: {}: {} -> {}", index, prompt, codeGenTypeEnum.getValue());
            });
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
