package com.example.codefather.mq.consumer;

import com.example.codefather.constant.RabbitMqConstant;
import com.example.codefather.model.entity.AppDeployTask;
import com.example.codefather.model.enums.AppDeployTaskStatusEnum;
import com.example.codefather.mq.exception.RetryableTaskException;
import com.example.codefather.mq.message.AppDeployTaskMessage;
import com.example.codefather.mq.producer.AppDeployTaskProducer;
import com.example.codefather.service.AppDeployTaskService;
import com.example.codefather.service.DeployTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 应用部署任务消费者。
 */
@Slf4j
@Component
public class AppDeployTaskConsumer {

    private static final int MAX_DEPLOY_RETRY_COUNT = 2;

    private static final int MAX_SCREENSHOT_RETRY_COUNT = 2;

    private final AppDeployTaskService appDeployTaskService;

    private final DeployTaskExecutor deployTaskExecutor;

    private final AppDeployTaskProducer appDeployTaskProducer;

    private final RedissonClient redissonClient;

    public AppDeployTaskConsumer(AppDeployTaskService appDeployTaskService,
                                 DeployTaskExecutor deployTaskExecutor,
                                 AppDeployTaskProducer appDeployTaskProducer,
                                 RedissonClient redissonClient) {
        this.appDeployTaskService = appDeployTaskService;
        this.deployTaskExecutor = deployTaskExecutor;
        this.appDeployTaskProducer = appDeployTaskProducer;
        this.redissonClient = redissonClient;
    }

    @RabbitListener(queues = RabbitMqConstant.APP_DEPLOY_QUEUE)
    public void consumeDeployTask(AppDeployTaskMessage message) {
        handleTask(message, true);
    }

    @RabbitListener(queues = RabbitMqConstant.APP_SCREENSHOT_QUEUE)
    public void consumeScreenshotTask(AppDeployTaskMessage message) {
        handleTask(message, false);
    }

    private void handleTask(AppDeployTaskMessage message, boolean deployStep) {
        Long taskId = message.getTaskId();
        String lockKey = "codefather:deploy:task:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        AppDeployTask task = null;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("任务正在被其他消费者处理，taskId={}", taskId);
                return;
            }
            task = appDeployTaskService.getById(taskId);
            if (task == null) {
                log.warn("部署任务不存在，taskId={}", taskId);
                return;
            }
            if (AppDeployTaskStatusEnum.SUCCESS.getValue().equals(task.getStatus())
                    || AppDeployTaskStatusEnum.FAILED.getValue().equals(task.getStatus())) {
                return;
            }
            if (deployStep && !AppDeployTaskStatusEnum.PENDING.getValue().equals(task.getStatus())
                    && !AppDeployTaskStatusEnum.DEPLOYING.getValue().equals(task.getStatus())) {
                log.info("跳过重复部署消息，taskId={}, status={}", taskId, task.getStatus());
                return;
            }
            if (!deployStep && !AppDeployTaskStatusEnum.SCREENSHOTTING.getValue().equals(task.getStatus())) {
                log.info("跳过非截图阶段消息，taskId={}, status={}", taskId, task.getStatus());
                return;
            }
            if (deployStep) {
                appDeployTaskService.updateTaskStatus(taskId, AppDeployTaskStatusEnum.DEPLOYING, null);
                deployTaskExecutor.executeDeploy(task);
                appDeployTaskService.resetRetryCount(taskId);
                appDeployTaskService.updateTaskStatus(taskId, AppDeployTaskStatusEnum.SCREENSHOTTING, null);
                appDeployTaskProducer.sendScreenshotTask(taskId, task.getAppId(), 0, null);
            } else {
                appDeployTaskService.updateTaskStatus(taskId, AppDeployTaskStatusEnum.SCREENSHOTTING, null);
                deployTaskExecutor.executeScreenshot(task);
                appDeployTaskService.updateTaskStatus(taskId, AppDeployTaskStatusEnum.SUCCESS, null);
            }
        } catch (RetryableTaskException e) {
            retryOrDeadLetter(task, deployStep, e.getMessage());
        } catch (Exception e) {
            failDirectly(task, deployStep, e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void retryOrDeadLetter(AppDeployTask task,
                                   boolean deployStep,
                                   String errorMessage) {
        if (task == null) {
            return;
        }
        int maxRetryCount = deployStep ? MAX_DEPLOY_RETRY_COUNT : MAX_SCREENSHOT_RETRY_COUNT;
        int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        if (currentRetryCount < maxRetryCount) {
            appDeployTaskService.incrementRetryCount(task.getId());
            if (deployStep) {
                appDeployTaskProducer.sendDeployTask(task.getId(), task.getAppId(), currentRetryCount + 1, errorMessage);
            } else {
                appDeployTaskProducer.sendScreenshotTask(task.getId(), task.getAppId(), currentRetryCount + 1, errorMessage);
            }
            log.warn("任务执行失败，准备重试，taskId={}, retryCount={}", task.getId(), currentRetryCount + 1);
            return;
        }
        AppDeployTaskMessage deadLetterMessage = AppDeployTaskMessage.builder()
                .taskId(task.getId())
                .appId(task.getAppId())
                .retryCount(currentRetryCount)
                .errorMessage(errorMessage)
                .build();
        appDeployTaskService.updateTaskStatus(task.getId(), AppDeployTaskStatusEnum.FAILED, errorMessage);
        if (deployStep) {
            appDeployTaskProducer.sendDeployDeadLetter(deadLetterMessage);
        } else {
            appDeployTaskProducer.sendScreenshotDeadLetter(deadLetterMessage);
        }
    }

    private void failDirectly(AppDeployTask task,
                              boolean deployStep,
                              String errorMessage) {
        if (task == null) {
            return;
        }
        appDeployTaskService.updateTaskStatus(task.getId(), AppDeployTaskStatusEnum.FAILED, errorMessage);
        AppDeployTaskMessage deadLetterMessage = AppDeployTaskMessage.builder()
                .taskId(task.getId())
                .appId(task.getAppId())
                .retryCount(task.getRetryCount())
                .errorMessage(errorMessage)
                .build();
        if (deployStep) {
            appDeployTaskProducer.sendDeployDeadLetter(deadLetterMessage);
        } else {
            appDeployTaskProducer.sendScreenshotDeadLetter(deadLetterMessage);
        }
    }
}
