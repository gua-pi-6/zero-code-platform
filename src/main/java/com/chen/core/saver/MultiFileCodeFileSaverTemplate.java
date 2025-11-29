package com.chen.core.saver;

import cn.hutool.core.util.StrUtil;
import com.chen.ai.model.MultiFileCodeResult;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 *
 * @author yupi
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFile(String baseDirPath, MultiFileCodeResult result) {
        // 保存 HTML 文件
        writeSingleFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeSingleFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeSingleFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void efficacyParams(MultiFileCodeResult result) {
        super.efficacyParams(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
