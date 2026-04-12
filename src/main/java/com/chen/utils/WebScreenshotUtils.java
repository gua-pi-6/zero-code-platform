package com.chen.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.constant.AppConstant;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 截取指定 URL 的网页截图
     *
     * @param url 要截取截图的网页 URL
     * @return 如果截图成功则返回 true，否则返回 false
     */
    public static String screenshotByUrl(String url){
        try {
            // 效验参数
            if(url == null || StrUtil.isBlank(url)){
                log.error("url 不能为空");
                return null;
            }

            // 访问页面
            webDriver.get(url);

            // 等待页面加载完成
            waitForPageLoad(webDriver);

            // 截取图片
            final byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            // 创建原始图片后缀
            String originImgLast = ".png";

            // 创建压缩图片后缀
            String compressImgLast = "_compressed.jpg";

            // 保存原始图片
            String saveImgPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + RandomUtil.randomString(5) + originImgLast;
            saveImg(saveImgPath, screenshot);

            // 压缩图片
            String compressImgPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + RandomUtil.randomString(5) + compressImgLast;
            compressImg(saveImgPath, compressImgPath);

            // 删除原始图片
            FileUtil.del(saveImgPath);
            return compressImgPath;
        } catch (Exception e) {
            log.error("截取图片失败: {}", e.getMessage());
            return null;
        }
    }



    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 保存图片到指定路径
     *
     * @param filePath 图片保存路径
     * @param imgBytes 图片字节数组
     * @return 图片保存路径
     */
    private static void saveImg(String filePath, byte[] imgBytes) {
        try {
            FileUtil.writeBytes(imgBytes, filePath);
            log.info("图片保存成功，路径：{}", filePath);

        } catch (Exception e) {
            log.error("图片保存失败，路径：{}", filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
        }
    }

    /**
     * 压缩图片
     *
     * @param originPath  原始图片路径
     * @param compressPath 压缩图片路径
     * @return 压缩图片路径
     */
    private static void compressImg(String originPath, String compressPath) {
        try {
            final float compressRatio = 0.3f;
            ImgUtil.compress(FileUtil.file(originPath), FileUtil.file(compressPath), compressRatio);
            log.info("图片压缩成功，压缩路径：{}", compressPath);

        } catch (Exception e) {
            log.error("图片压缩失败，原始路径：{}，压缩路径：{}", originPath, compressPath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }

    }

    /**
     * 等待页面加载完成
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }


    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

}
