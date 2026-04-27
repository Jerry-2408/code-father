package com.example.codefather.mq.exception;

/**
 * 可重试任务异常。
 */
public class RetryableTaskException extends RuntimeException {

    public RetryableTaskException(String message) {
        super(message);
    }

    public RetryableTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
