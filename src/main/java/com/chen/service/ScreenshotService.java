package com.chen.service;

public interface ScreenshotService {

    /**
     * 生成并上传网页截图
     *
     * @param webUrl 要截取截图的网页 URL
     * @return 截图在 COS 中的访问 URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
