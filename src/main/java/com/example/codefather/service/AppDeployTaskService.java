package com.example.codefather.service;

import com.example.codefather.model.entity.AppDeployTask;
import com.example.codefather.model.enums.AppDeployTaskStatusEnum;
import com.example.codefather.model.vo.app.AppDeployTaskVO;
import com.mybatisflex.core.service.IService;

/**
 * 应用部署任务服务。
 */
public interface AppDeployTaskService extends IService<AppDeployTask> {

    boolean hasRunningTask(Long appId);

    AppDeployTask createTask(Long appId, Long userId, String deployKey);

    AppDeployTaskVO getTaskVO(AppDeployTask task);

    void updateTaskStatus(Long taskId, AppDeployTaskStatusEnum status, String errorMessage);

    void incrementRetryCount(Long taskId);

    void resetRetryCount(Long taskId);
}
