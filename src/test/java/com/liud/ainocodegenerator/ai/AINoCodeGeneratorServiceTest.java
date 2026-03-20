package com.liud.ainocodegenerator.ai;

import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AINoCodeGeneratorServiceTest {
    @Resource
    private AINoCodeGeneratorService aiNoCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult res = aiNoCodeGeneratorService.generateHtmlCode("制作一个刘东的博客，要求20行");
        Assertions.assertNotNull(res);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult res = aiNoCodeGeneratorService.generateMultiFileCode("制作一个刘东的留言板，要求20行");
        Assertions.assertNotNull(res);
    }
}