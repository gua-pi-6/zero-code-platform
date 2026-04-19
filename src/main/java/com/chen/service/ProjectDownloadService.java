package com.chen.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 提供生成项目压缩下载能力。
 */
public interface ProjectDownloadService {

    /**
     * 将指定项目目录以 zip 压缩包形式输出。
     *
     * @param projectRootPath 项目根目录
     * @param downloadFileName 输出文件名（不含扩展名）
     * @param response Servlet 响应对象
     */
    void downloadProjectAsZip(String projectRootPath, String downloadFileName, HttpServletResponse response);
}
