package com.chen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.manager.CosManager;
import com.chen.service.ScreenshotService;
import com.chen.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ScreenshotImpl implements ScreenshotService {


    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 参数效验
        ThrowUtils.throwIf(webUrl == null || StrUtil.isBlank(webUrl), ErrorCode.OPERATION_ERROR, "网站路径不能为空");
        log.info("开始截取网页截图: {}", webUrl);

        // 截取网页截图
        String localFilePath = WebScreenshotUtils.screenshotByUrl(webUrl);
        ThrowUtils.throwIf(localFilePath == null || StrUtil.isBlank(localFilePath), ErrorCode.OPERATION_ERROR, "截取网页截图失败");
        log.info("截取网页截图成功, localFilePath: {}", localFilePath);

        try {
            // 把截图上传到 COS
            String cosFilePath = uploadScreenshotToCos(new File(localFilePath));
            return cosFilePath;
        } finally {
            // 删除本地文件
            boolean isDeleted = deleteFile(new File(localFilePath));
            ThrowUtils.throwIf(!isDeleted, ErrorCode.OPERATION_ERROR, "删除本地文件失败");
            log.info("删除本地文件成功, localFilePath: {}", localFilePath);
        }


    }

    /**
     * 上传截图到 COS
     *
     * @param filePath 要上传的文件路径
     * @return COS 上的文件路径
     */
    private String uploadScreenshotToCos(File filePath) {
        // 校验文件路径是否存在
        ThrowUtils.throwIf(filePath == null || !filePath.exists(), ErrorCode.OPERATION_ERROR, "文件路径不存在");

        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString(true).substring(0, 8) + "_compressed.jpg";

        // 构建key
        String key = generateScreenshotKey(fileName);

        // 上传到COS
        return cosManager.uploadFileToCos(filePath, key);
    }

    /**
     * 删除本地文件
     *
     * @param filePath 要删除的文件路径
     * @return 是否删除成功
     */
    private boolean deleteFile(File filePath) {
        if (!FileUtil.del(filePath)) {
            log.error("删除文件失败: {}", filePath);
            return false;
        }
        log.info("删除文件成功: {}", filePath);
        return true;
    }

    /**
     * 生成COS对象的存储键
     * 格式: screenshot/yyyy/MM/dd/filename.jpg
     *
     * @param fileName 文件名
     * @return 截图文件路径
     */
    private String generateScreenshotKey(String fileName) {
        // 定义对象键
        String dataPath = "screenshot/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        return dataPath + "/" + fileName;
    }
}

