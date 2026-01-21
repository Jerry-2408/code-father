package com.example.codefather.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    private static final String REQUEST_START_TIME_KEY = "request_start_time";

    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;


    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 在AI模型上下文中记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // 从AI模型上下文中获取监控上下文
        MonitorContext context = MonitorContextHolder.getContext();
        if (context == null) {
            log.warn("MonitorContext 缺失，无法记录AI监控指标");
            return;
        }
        // 保存监控上下文
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);
        // 从监控上下文中获取信息
        String userId = context.getUserId();
        String appId = context.getAppId();
        // 获取模型名称
        String modelName = requestContext.chatRequest().modelName();
        // 记录开始请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从AI模型上下文中获取监控上下文
        MonitorContext context = (MonitorContext) responseContext.attributes().get(MONITOR_CONTEXT_KEY);
        if (context == null) {
            log.warn("MonitorContext 缺失，无法记录AI监控指标");
            return;
        }
        // 从监控上下文中获取信息
        String userId = context.getUserId();
        String appId = context.getAppId();
        // 获取模型名称
        String modelName = responseContext.chatResponse().modelName();
        // 获取请求开始时间
        Instant requestStartTime = (Instant) responseContext.attributes().get(REQUEST_START_TIME_KEY);
        // 记录成功请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "succeeded");
        // 记录响应时间
        recordResponseTime(userId, appId, modelName, requestStartTime);
        // 记录token使用情况
        recordTokenUsage(userId, appId, modelName, responseContext.chatResponse().metadata().tokenUsage());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从AI模型上下文中获取监控上下文
        MonitorContext context = (MonitorContext) errorContext.attributes().get(MONITOR_CONTEXT_KEY);
        // 从监控上下文中获取信息
        String userId = context.getUserId();
        String appId = context.getAppId();
        // 获取模型名称
        String modelName = errorContext.chatRequest().modelName();
        // 获取错误信息
        String errorMessage = errorContext.error().getMessage();
        // 获取请求开始时间
        Instant requestStartTime = (Instant) errorContext.attributes().get(REQUEST_START_TIME_KEY);
        // 记录失败请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "failed");
        // 记录错误信息
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        // 记录响应时间
        recordResponseTime(userId, appId, modelName, requestStartTime);
    }

    /**
     * 记录响应时间
     */
    private void recordResponseTime(String userId, String appId, String modelName, Instant requestStartTime) {
        Instant responseTime = Instant.now();
        Duration duration = Duration.between(requestStartTime, responseTime);
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, duration);
    }

    private void recordTokenUsage(String userId, String appId, String modelName, TokenUsage tokenUsage) {
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
