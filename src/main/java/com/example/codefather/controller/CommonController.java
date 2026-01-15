package com.example.codefather.controller;

import cn.hutool.core.lang.UUID;
import com.example.codefather.common.BaseResponse;
import com.example.codefather.common.ResultUtils;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.manager.CosManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/common")
public class CommonController {

    @Resource
    private CosManager cosManager;

    @PostMapping("/upload")
    public BaseResponse<String> upload(MultipartFile file) {
        String dataPath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String[] split = file.getOriginalFilename().split("\\.");
        String suffix = split[split.length - 1];
        Set<String> suffixes = Set.of("png", "jpg", "jpeg", "webp");
        // 如果文件不符合图片格式，抛出异常
        if (!suffixes.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式错误");
        }
        String key = String.format("/avatar/%s/%s.%s", dataPath, UUID.randomUUID(), suffix);
        String url = cosManager.uploadFileWithMultipartFile(key, file);
        return ResultUtils.success(url);
    }
}
