package com.example.codefather.core.saver;

import cn.hutool.core.util.StrUtil;
import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存模板
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult htmlCodeResult, String dirPath) {
        // 保存HTML文件
        writeToFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult codeResult) {
        super.validateInput(codeResult);
        if (StrUtil.isBlank(codeResult.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
