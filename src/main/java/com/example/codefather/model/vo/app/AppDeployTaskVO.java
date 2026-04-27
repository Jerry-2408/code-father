package com.example.codefather.model.vo.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署任务视图。
 */
@Data
public class AppDeployTaskVO implements Serializable {

    private Long taskId;

    private Long appId;

    private String deployKey;

    private String deployUrl;

    private String status;

    private Integer retryCount;

    private String errorMessage;

    private static final long serialVersionUID = 1L;
}
