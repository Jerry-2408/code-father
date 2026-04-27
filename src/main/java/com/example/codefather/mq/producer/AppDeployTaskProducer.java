package com.example.codefather.mq.producer;

import com.example.codefather.constant.RabbitMqConstant;
import com.example.codefather.mq.message.AppDeployTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 应用部署任务消息生产者。
 */
@Slf4j
@Component
public class AppDeployTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public AppDeployTaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDeployTask(Long taskId, Long appId, Integer retryCount, String errorMessage) {
        AppDeployTaskMessage message = buildMessage(taskId, appId, retryCount, errorMessage);
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                RabbitMqConstant.APP_DEPLOY_ROUTING_KEY,
                message
        );
        log.info("发送部署任务消息成功，taskId={}", taskId);
    }

    public void sendScreenshotTask(Long taskId, Long appId, Integer retryCount, String errorMessage) {
        AppDeployTaskMessage message = buildMessage(taskId, appId, retryCount, errorMessage);
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                RabbitMqConstant.APP_SCREENSHOT_ROUTING_KEY,
                message
        );
        log.info("发送截图任务消息成功，taskId={}", taskId);
    }

    public void sendDeployDeadLetter(AppDeployTaskMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                RabbitMqConstant.APP_DEPLOY_DLQ_ROUTING_KEY,
                message
        );
        log.warn("部署任务进入死信队列，taskId={}", message.getTaskId());
    }

    public void sendScreenshotDeadLetter(AppDeployTaskMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                RabbitMqConstant.APP_SCREENSHOT_DLQ_ROUTING_KEY,
                message
        );
        log.warn("截图任务进入死信队列，taskId={}", message.getTaskId());
    }

    private AppDeployTaskMessage buildMessage(Long taskId, Long appId, Integer retryCount, String errorMessage) {
        return AppDeployTaskMessage.builder()
                .taskId(taskId)
                .appId(appId)
                .retryCount(retryCount)
                .errorMessage(errorMessage)
                .sentTime(LocalDateTime.now())
                .build();
    }
}
