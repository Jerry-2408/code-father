package com.example.codefather.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codefather.constant.AppConstant;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存模板类（模板模式）
 *
 * @param <T> 代码结果对象类型
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 保存代码（统一模板方法）
     *
     * @param codeResult 代码结果对象
     * @param appId 应用ID
     * @return 保存后的文件目录
     */
    public final File saveCode(T codeResult, Long appId) {
        // 1. 验证输入
        validateInput(codeResult);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        // 3. 保存文件
        saveFiles(codeResult, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param codeResult 代码结果对象
     */
    protected void validateInput(T codeResult) {
        if (codeResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_snowID
     *
     * @param appId 应用ID
     * @return 唯一目录路径
     */
    protected final String buildUniqueDir(Long appId) {
        // 获取生成代码类型
        String codeType = getCodeType().getValue();
        // 构建目录名称
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        // 创建目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取代码生成类型（由子类实现）
     *
     * @return 代码生成类型枚举
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件（由子类实现）
     *
     * @param codeResult 代码结果对象
     * @param dirPath 目录路径
     */
    protected abstract void saveFiles(T codeResult, String dirPath);

    /**
     * 写入单个文件
     *
     * @param dirPath 目录路径
     * @param fileName 文件名
     * @param content 文件内容
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}

