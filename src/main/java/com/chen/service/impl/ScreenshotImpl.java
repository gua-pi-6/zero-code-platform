package com.chen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.manager.CosManager;
import com.chen.service.ScreenshotService;
import com.chen.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 在本地生成页面截图，并上传到 COS。
 */
@Slf4j
@Service
public class ScreenshotImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    /**
     * 生成页面截图并上传到 COS。
     *
     * @param webUrl 目标页面地址
     * @return 上传后的截图地址
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 调用截图工具前先校验目标地址。
        ThrowUtils.throwIf(webUrl == null || StrUtil.isBlank(webUrl), ErrorCode.OPERATION_ERROR, "网站路径不能为空");
        log.info("开始截取网页截图 {}", webUrl);

        // 先在本地截取页面截图。
        String localFilePath = WebScreenshotUtils.screenshotByUrl(webUrl);
        ThrowUtils.throwIf(localFilePath == null || StrUtil.isBlank(localFilePath), ErrorCode.OPERATION_ERROR, "截取网页截图失败");
        log.info("截取网页截图成功, localFilePath: {}", localFilePath);

        try {
            // 将临时截图文件上传到 COS。
            return uploadScreenshotToCos(new File(localFilePath));
        } finally {
            // 上传结束后，无论成功失败都删除本地临时文件。
            boolean isDeleted = deleteFile(new File(localFilePath));
            ThrowUtils.throwIf(!isDeleted, ErrorCode.OPERATION_ERROR, "删除本地文件失败");
            log.info("删除本地文件成功, localFilePath: {}", localFilePath);
        }
    }

    /**
     * 将截图文件上传到 COS。
     *
     * @param filePath 截图文件
     * @return 上传后的文件地址或 Key
     */
    private String uploadScreenshotToCos(File filePath) {
        // 上传前先校验生成的截图文件是否存在。
        ThrowUtils.throwIf(filePath == null || !filePath.exists(), ErrorCode.OPERATION_ERROR, "文件路径不存在");

        // 为上传对象生成紧凑的随机文件名。
        String fileName = UUID.randomUUID().toString(true).substring(0, 8) + "_compressed.jpg";

        // 按当前日期组装最终的 COS 对象 Key。
        String key = generateScreenshotKey(fileName);
        return cosManager.uploadFileToCos(filePath, key);
    }

    /**
     * 删除本地临时文件。
     *
     * @param filePath 本地文件
     * @return 是否删除成功
     */
    private boolean deleteFile(File filePath) {
        // 删除临时文件，并记录删除结果。
        if (!FileUtil.del(filePath)) {
            log.error("删除文件失败: {}", filePath);
            return false;
        }
        log.info("删除文件成功: {}", filePath);
        return true;
    }

    /**
     * 构造截图文件在 COS 中使用的 Key。
     *
     * @param fileName 上传后的文件名
     * @return COS 对象 Key
     */
    private String generateScreenshotKey(String fileName) {
        // 按日期分层存储截图，便于后续管理。
        String dataPath = "screenshot/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return dataPath + "/" + fileName;
    }
}
