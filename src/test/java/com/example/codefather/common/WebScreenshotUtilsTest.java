package com.example.codefather.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String s = WebScreenshotUtils.saveWebPageScreenshot("https://www.baidu.com");
        Assertions.assertNotNull(s);
    }
}