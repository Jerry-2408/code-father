package com.example.codefather.config;

import com.example.codefather.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 配置。
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange appDeployExchange() {
        return new DirectExchange(RabbitMqConstant.APP_DEPLOY_EXCHANGE, true, false);
    }

    @Bean
    public Queue appDeployQueue() {
        return new Queue(
                RabbitMqConstant.APP_DEPLOY_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                        "x-dead-letter-routing-key", RabbitMqConstant.APP_DEPLOY_DLQ_ROUTING_KEY
                )
        );
    }

    @Bean
    public Queue appDeployDlq() {
        return new Queue(RabbitMqConstant.APP_DEPLOY_DLQ, true);
    }

    @Bean
    public Queue appScreenshotQueue() {
        return new Queue(
                RabbitMqConstant.APP_SCREENSHOT_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", RabbitMqConstant.APP_DEPLOY_EXCHANGE,
                        "x-dead-letter-routing-key", RabbitMqConstant.APP_SCREENSHOT_DLQ_ROUTING_KEY
                )
        );
    }

    @Bean
    public Queue appScreenshotDlq() {
        return new Queue(RabbitMqConstant.APP_SCREENSHOT_DLQ, true);
    }

    @Bean
    public Binding appDeployBinding(@Qualifier("appDeployQueue") Queue appDeployQueue,
                                    DirectExchange appDeployExchange) {
        return BindingBuilder.bind(appDeployQueue)
                .to(appDeployExchange)
                .with(RabbitMqConstant.APP_DEPLOY_ROUTING_KEY);
    }

    @Bean
    public Binding appDeployDlqBinding(@Qualifier("appDeployDlq") Queue appDeployDlq,
                                       DirectExchange appDeployExchange) {
        return BindingBuilder.bind(appDeployDlq)
                .to(appDeployExchange)
                .with(RabbitMqConstant.APP_DEPLOY_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding appScreenshotBinding(@Qualifier("appScreenshotQueue") Queue appScreenshotQueue,
                                        DirectExchange appDeployExchange) {
        return BindingBuilder.bind(appScreenshotQueue)
                .to(appDeployExchange)
                .with(RabbitMqConstant.APP_SCREENSHOT_ROUTING_KEY);
    }

    @Bean
    public Binding appScreenshotDlqBinding(@Qualifier("appScreenshotDlq") Queue appScreenshotDlq,
                                           DirectExchange appDeployExchange) {
        return BindingBuilder.bind(appScreenshotDlq)
                .to(appDeployExchange)
                .with(RabbitMqConstant.APP_SCREENSHOT_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMqMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
