package com.chen.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebScreenshotUtilsTest {

    @Test
    void screenshotByUrl() {
        String result = WebScreenshotUtils.screenshotByUrl("https://www.codefather.cn/");
        Assertions.assertNotNull(result);
    }
}