package com.example.codefather.monitor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitorContextHolder {

    /**
     * 线程本地变量，必须要用InheritableThreadLocal，第一次onRequest是当前线程，但是如果有工具调用，会有多次子线程调用onRequest
     */
    private static final InheritableThreadLocal<MonitorContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置监控上下文
     */
    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前监控上下文
     */
    public static MonitorContext getContext() {return CONTEXT_HOLDER.get();
    }

    /**
     * 清除监控上下文
     */
    public static void clearContext() {
//        log.info("before remove context: {}", CONTEXT_HOLDER.get());
        CONTEXT_HOLDER.remove();
//        log.info("after remove context: {}", CONTEXT_HOLDER.get());
    }
}
