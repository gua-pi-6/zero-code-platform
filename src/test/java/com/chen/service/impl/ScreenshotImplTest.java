package com.chen.service.impl;

import com.chen.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestParam;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScreenshotImplTest {

    @Resource
    private ScreenshotService screenshotService;

    @Test
    void generateAndUploadScreenshot() {
        String string = screenshotService.generateAndUploadScreenshot("https://www.codefather.cn/");
        Assertions.assertNotNull(string);
    }
}