package com.chen.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.constant.AppConstant;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 代码文件保存模板类
 */
public abstract class CodeFileSaverTemplate<T> {
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    public final File saveCode(T codeType, Long appId) {
        // 效验参数
        efficacyParams(codeType);
        // 创建唯一目录
        String baseDirPath = this.createUniqueDir(appId);
        // 保存文件
        this.saveFile(baseDirPath, codeType);
        // 返回目录
        return new File(baseDirPath);
    }

    /**
     * 写入单文件
     */
    protected final void writeSingleFile(String baseDirPath, String filename, String content){
        if (StrUtil.isNotBlank(content)) {
            String filePath = baseDirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 效验参数
     */
    protected void efficacyParams(T codeType) {
        if (codeType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无解析后的代码对象或解析错误");
        }
    }

    /**
     * 创建唯一目录
     */
    protected final String createUniqueDir(Long appId){
        String uniqueDirName = StrUtil.format("{}_{}", this.getCodeType().getValue(), appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取代码生成类型枚举
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件
     */
    protected abstract void saveFile(String baseDirPath, T codeType);


}
