package com.chen.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * 以过滤后的 zip 压缩包形式输出生成项目目录。
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 导出压缩包时需要排除的目录和文件名。
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 导出压缩包时需要排除的文件扩展名。
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 将生成后的项目目录以 zip 压缩包形式输出。
     *
     * @param projectRootPath 项目根目录
     * @param downloadFileName 输出文件名（不含扩展名）
     * @param response Servlet 响应对象
     */
    @Override
    public void downloadProjectAsZip(String projectRootPath, String downloadFileName, HttpServletResponse response) {
        // 打开响应输出流前，先校验项目路径和输出文件名。
        ThrowUtils.throwIf(projectRootPath == null || StrUtil.isBlank(projectRootPath), ErrorCode.PARAMS_ERROR, "项目根目录不能为空");
        ThrowUtils.throwIf(downloadFileName == null || downloadFileName.isEmpty(), ErrorCode.PARAMS_ERROR, "文件名不能为空");

        // 定位项目目录，并校验目录在磁盘上真实存在。
        File projectDir = new File(projectRootPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR, "项目目录不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "项目目录不是一个目录");
        log.info("开始压缩项目 {}", projectRootPath);

        // 设置响应头，通知浏览器以 zip 文件形式下载。
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", downloadFileName));

        // 构造文件过滤器，排除生成物和本地环境相关文件。
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());

        try {
            // 将过滤后的压缩包内容直接写入 Servlet 响应流。
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("项目压缩完成: {}", downloadFileName);
        } catch (Exception e) {
            log.error("压缩项目失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩项目失败");
        }
    }

    /**
     * 判断文件系统路径是否允许出现在导出的压缩包中。
     *
     * @param projectRoot 项目根路径
     * @param fullPath 完整文件路径
     * @return 是否允许导出
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 先将完整路径转换为项目根目录下的相对路径。
        Path relativePath = projectRoot.relativize(fullPath);

        // 只要任一路径片段命中忽略规则，就直接拒绝导出。
        for (Path part : relativePath) {
            String partName = part.toString();
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }
}
