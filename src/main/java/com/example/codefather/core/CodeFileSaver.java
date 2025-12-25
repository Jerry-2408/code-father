package com.example.codefather.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.ai.model.MultiFileCodeResult;
import com.example.codefather.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Deprecated
public class CodeFileSaver {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";


    /**
     * 保存 HtmlCodeResult
     *
     * @param htmlCodeResult
     * @return 保存后的文件目录
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String dirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    /**
     * 保存 MutilFileCodeResult
     *
     * @param multiFileCodeResult
     * @return 保存后的文件目录
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        String dirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        writeToFile(dirPath, "style.css", multiFileCodeResult.getCssCode());
        writeToFile(dirPath, "script.js", multiFileCodeResult.getJsCode());
        return new File(dirPath);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_snowID
     *
     * @param bizType 业务类型
     * @return 唯一目录路径
     */
    private static String buildUniqueDir(String bizType) {
        // 构建目录名称
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        // 创建目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     *
     * @param dirPath 目录路径
     * @param fileName 文件名
     * @param content 文件内容
     */
    private static void writeToFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
