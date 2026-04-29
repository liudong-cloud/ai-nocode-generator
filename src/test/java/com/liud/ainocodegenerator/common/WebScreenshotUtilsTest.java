package com.liud.ainocodegenerator.common;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String s = WebScreenshotUtils.saveWebPageScreenshot("https://www.google.cn");
        Assert.notEmpty(s);
    }
}