package com.example.codefather.manager;

import com.example.codefather.config.CosClientConfig;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param inputStream 文件流
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, InputStream inputStream) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, inputStream,  null);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件对象到COS并返回访问的URL
     * @param key 唯一键
     * @param file 文件
     * @return 访问的URL
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        // 构建对象访问Url
        if (result != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("文件上传成功：{} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传失败：{}", file.getName());
            return null;
        }
    }

    /**
     * 上传文件对象到COS并返回访问的URL
     * @param key 唯一键
     * @param file 多binary文件
     * @return 访问的URL
     */
    public String uploadFileWithMultipartFile(String key, MultipartFile file) {
        // 上传文件
        PutObjectResult result = null;
        try {
            result = putObject(key, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
        // 构建对象访问Url
        if (result != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("文件上传成功：{} -> {}", file.getOriginalFilename(), url);
            return url;
        } else {
            log.error("文件上传失败：{}", file.getOriginalFilename());
            return null;
        }
    }
}
