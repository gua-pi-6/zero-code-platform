package com.chen.controller;

import com.chen.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

/**
 * 提供本地生成预览资源的静态访问能力。
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    /**
     * 生成应用预览资源的根目录。
     */
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 读取生成后的静态资源，并处理目录访问重定向逻辑。
     *
     * @param deployKey 应用预览标识
     * @param request Servlet 请求对象
     * @return 预览资源响应
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(@PathVariable String deployKey, HttpServletRequest request) {
        try {
            // 先解析相对于预览根目录的资源路径。
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());

            // 对未携带斜杠的目录访问做重定向处理。
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }

            // 目录访问默认回落到 index.html。
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 定位磁盘上的实际文件，并校验文件是否存在。
            String filePath = PREVIEW_ROOT_DIR + "/" + deployKey + resourcePath;
            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 按识别出的内容类型回传目标文件。
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取预览响应使用的内容类型。
     *
     * @param filePath 资源文件路径
     * @return 内容类型
     */
    private String getContentTypeWithCharset(String filePath) {
        // 文本类资源统一指定 UTF-8，避免预览乱码。
        if (filePath.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (filePath.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (filePath.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (filePath.endsWith(".png")) {
            return "image/png";
        }
        if (filePath.endsWith(".jpg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
