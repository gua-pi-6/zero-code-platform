package com.chen.core.saver;

import cn.hutool.core.util.StrUtil;
import com.chen.ai.model.HtmlCodeResult;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存模板类
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFile(String baseDirPath, HtmlCodeResult codeType) {
        writeSingleFile(baseDirPath, "index.html", codeType.getHtmlCode());
    }

    @Override
    protected void efficacyParams(HtmlCodeResult codeType) {
        super.efficacyParams(codeType);

        if (StrUtil.isBlank(codeType.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
