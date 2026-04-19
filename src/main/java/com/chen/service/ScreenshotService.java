package com.chen.service;

/**
 * 提供已部署应用的截图生成能力。
 */
public interface ScreenshotService {

    /**
     * 为指定页面生成截图并上传。
     *
     * @param webUrl 目标页面地址
     * @return 上传后的截图地址
     */
    String generateAndUploadScreenshot(String webUrl);
}
