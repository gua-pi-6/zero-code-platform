package com.chen.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {
    /**
     * 下载项目作为 ZIP 文件
     *
     * @param projectRootPath    项目根目录路径
     * @param downloadFileName 下载的文件名
     * @param response         HttpServletResponse 用于返回 ZIP 文件
     */
    void downloadProjectAsZip(String projectRootPath, String downloadFileName, HttpServletResponse response);
}
