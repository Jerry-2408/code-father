package com.example.codefather.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codefather.constant.AppConstant;
import com.example.codefather.core.builder.VueProjectBuilder;
import com.example.codefather.model.entity.App;
import com.example.codefather.model.entity.AppDeployTask;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import com.example.codefather.mq.exception.RetryableTaskException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 部署任务执行器。
 */
@Slf4j
@Service
public class DeployTaskExecutor {

    private final AppService appService;

    private final ScreenshotService screenshotService;

    private final VueProjectBuilder vueProjectBuilder;

    @Value("${code.deploy-host}")
    private String deployHost;

    public DeployTaskExecutor(AppService appService,
                              ScreenshotService screenshotService,
                              VueProjectBuilder vueProjectBuilder) {
        this.appService = appService;
        this.screenshotService = screenshotService;
        this.vueProjectBuilder = vueProjectBuilder;
    }

    /**
     * 执行部署主流程。
     */
    public void executeDeploy(AppDeployTask task) {
        App app = getAppOrThrow(task.getAppId());
        String sourceDirPath = buildSourceDirPath(app);
        File sourceDir = prepareSourceDir(app, sourceDirPath);
        File deployDir = new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + task.getDeployKey());
        try {
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            throw new RetryableTaskException("复制部署文件失败", e);
        }
        App updateApp = new App();
        updateApp.setId(app.getId());
        updateApp.setDeployKey(task.getDeployKey());
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updated = appService.updateById(updateApp);
        if (!updated) {
            throw new RetryableTaskException("更新应用部署信息失败");
        }
        log.info("应用部署完成，taskId={}, appId={}", task.getId(), task.getAppId());
    }

    /**
     * 执行部署后的截图流程。
     */
    public void executeScreenshot(AppDeployTask task) {
        if (StrUtil.isBlank(task.getDeployKey())) {
            throw new IllegalStateException("部署标识不存在，无法生成截图");
        }
        String appUrl = String.format("%s/%s", deployHost, task.getDeployKey());
        String screenshotUrl;
        try {
            screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
        } catch (Exception e) {
            throw new RetryableTaskException("生成应用截图失败", e);
        }
        App updateApp = new App();
        updateApp.setId(task.getAppId());
        updateApp.setCover(screenshotUrl);
        boolean updated = appService.updateById(updateApp);
        if (!updated) {
            throw new RetryableTaskException("更新应用封面失败");
        }
        log.info("应用截图完成，taskId={}, appId={}", task.getId(), task.getAppId());
    }

    private App getAppOrThrow(Long appId) {
        App app = appService.getById(appId);
        if (app == null) {
            throw new IllegalStateException("应用不存在");
        }
        return app;
    }

    private String buildSourceDirPath(App app) {
        String sourceDirName = app.getCodeGenType() + "_" + app.getId();
        return AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
    }

    private File prepareSourceDir(App app, String sourceDirPath) {
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalStateException("应用代码不存在，请先生成代码");
        }
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (codeGenTypeEnum == null) {
            throw new IllegalStateException("不支持的代码生成类型");
        }
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            if (!buildSuccess) {
                throw new IllegalStateException("Vue项目构建出错");
            }
            sourceDir = new File(sourceDirPath, "dist");
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                throw new IllegalStateException("Vue项目构建完成但dist目录不存在");
            }
        }
        return sourceDir;
    }
}
