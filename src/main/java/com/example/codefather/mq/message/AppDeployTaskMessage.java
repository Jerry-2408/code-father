package com.example.codefather.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用部署任务消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDeployTaskMessage implements Serializable {

    private Long taskId;

    private Long appId;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime sentTime;

    private static final long serialVersionUID = 1L;
}
