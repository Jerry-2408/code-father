package com.example.codefather.service.impl;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codefather.utils.WebScreenshotUtils;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.manager.CosManager;
import com.example.codefather.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    public ScreenshotServiceImpl(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    /**
     * 生成网页截图并上传
     * @param webUrl 网页地址
     * @return 访问的URL
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页地址不能为空");
        // 1. 生成本地截图
        log.info("开始生成网页截图：{}", webUrl);
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        try {
            // 2. 上传到对象存储
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.SYSTEM_ERROR, "截图上传对象存储失败");
            log.info("截图生成并上传成功：{} -> {}", webUrl, cosUrl);
            return cosUrl;
        } finally {
            // 3. 清理本地文件
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 上传截图到对象存储
     * @param localScreenshotPath 本地截图路径
     * @return 访问的URL
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        // 截图文件
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在：{}", localScreenshotPath);
            return null;
        }
        // 生成cos对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的对象存储键
     * @param fileName 文件名
     * @return 对象存储键 格式：/screenshots/yyyy/MM/dd/fileName.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String dataPath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", dataPath, fileName);
    }

    /**
     * 清理本地文件
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("清理本地截图文件成功：{}", localFilePath);
        }
    }
}
