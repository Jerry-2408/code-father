package com.example.codefather.constant;

/**
 * RabbitMQ 常量。
 */
public interface RabbitMqConstant {

    String APP_DEPLOY_EXCHANGE = "app.deploy.exchange";

    String APP_DEPLOY_QUEUE = "app.deploy.queue";

    String APP_DEPLOY_ROUTING_KEY = "app.deploy";

    String APP_DEPLOY_DLQ = "app.deploy.dlq";

    String APP_DEPLOY_DLQ_ROUTING_KEY = "app.deploy.dlq";

    String APP_SCREENSHOT_QUEUE = "app.screenshot.queue";

    String APP_SCREENSHOT_ROUTING_KEY = "app.screenshot";

    String APP_SCREENSHOT_DLQ = "app.screenshot.dlq";

    String APP_SCREENSHOT_DLQ_ROUTING_KEY = "app.screenshot.dlq";
}
