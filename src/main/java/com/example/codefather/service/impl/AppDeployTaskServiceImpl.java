package com.example.codefather.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.mapper.AppDeployTaskMapper;
import com.example.codefather.model.entity.AppDeployTask;
import com.example.codefather.model.enums.AppDeployTaskStatusEnum;
import com.example.codefather.model.vo.app.AppDeployTaskVO;
import com.example.codefather.service.AppDeployTaskService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用部署任务服务实现。
 */
@Service
public class AppDeployTaskServiceImpl extends ServiceImpl<AppDeployTaskMapper, AppDeployTask> implements AppDeployTaskService {

    private static final List<String> RUNNING_STATUSES = List.of(
            AppDeployTaskStatusEnum.PENDING.getValue(),
            AppDeployTaskStatusEnum.DEPLOYING.getValue(),
            AppDeployTaskStatusEnum.SCREENSHOTTING.getValue()
    );

    @Value("${code.deploy-host}")
    private String deployHost;

    @Override
    public boolean hasRunningTask(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(AppDeployTask::getAppId, appId)
                .in(AppDeployTask::getStatus, RUNNING_STATUSES);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public AppDeployTask createTask(Long appId, Long userId, String deployKey) {
        AppDeployTask task = AppDeployTask.builder()
                .appId(appId)
                .userId(userId)
                .deployKey(deployKey)
                .status(AppDeployTaskStatusEnum.PENDING.getValue())
                .retryCount(0)
                .build();
        boolean saved = this.save(task);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建部署任务失败");
        }
        return task;
    }

    @Override
    public AppDeployTaskVO getTaskVO(AppDeployTask task) {
        if (task == null) {
            return null;
        }
        AppDeployTaskVO taskVO = new AppDeployTaskVO();
        taskVO.setTaskId(task.getId());
        taskVO.setAppId(task.getAppId());
        taskVO.setDeployKey(task.getDeployKey());
        if (StrUtil.isNotBlank(task.getDeployKey())) {
            taskVO.setDeployUrl(String.format("%s/%s", deployHost, task.getDeployKey()));
        }
        taskVO.setStatus(task.getStatus());
        taskVO.setRetryCount(task.getRetryCount());
        taskVO.setErrorMessage(task.getErrorMessage());
        return taskVO;
    }

    @Override
    public void updateTaskStatus(Long taskId, AppDeployTaskStatusEnum status, String errorMessage) {
        AppDeployTask updateTask = new AppDeployTask();
        updateTask.setId(taskId);
        updateTask.setStatus(status.getValue());
        updateTask.setErrorMessage(StrUtil.nullToEmpty(errorMessage));
        boolean updated = this.updateById(updateTask);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新部署任务状态失败");
        }
    }

    @Override
    public void incrementRetryCount(Long taskId) {
        AppDeployTask task = this.getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "部署任务不存在");
        }
        AppDeployTask updateTask = new AppDeployTask();
        updateTask.setId(taskId);
        updateTask.setRetryCount(task.getRetryCount() + 1);
        boolean updated = this.updateById(updateTask);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新部署任务重试次数失败");
        }
    }

    @Override
    public void resetRetryCount(Long taskId) {
        AppDeployTask updateTask = new AppDeployTask();
        updateTask.setId(taskId);
        updateTask.setRetryCount(0);
        boolean updated = this.updateById(updateTask);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重置部署任务重试次数失败");
        }
    }
}
