package com.example.codefather.core.saver;

import cn.hutool.core.util.StrUtil;
import com.example.codefather.ai.model.MultiFileCodeResult;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存模板
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult multiFileCodeResult, String dirPath) {
        // 保存HTML文件
        writeToFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        // 保存CSS文件
        writeToFile(dirPath, "style.css", multiFileCodeResult.getCssCode());
        // 保存JS文件
        writeToFile(dirPath, "script.js", multiFileCodeResult.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult codeResult) {
        super.validateInput(codeResult);
        if (StrUtil.isBlank(codeResult.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
