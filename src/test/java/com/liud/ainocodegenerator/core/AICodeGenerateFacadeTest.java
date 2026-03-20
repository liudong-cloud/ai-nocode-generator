package com.liud.ainocodegenerator.core;

import com.liud.ainocodegenerator.ai.AINoCodeGeneratorService;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AICodeGenerateFacadeTest {
    @Resource
    private AICodeGenerateFacade aiCodeGenerateFacade;

    @Test
    void generateAICodeAndSave() {
        File file = aiCodeGenerateFacade.generateAICodeAndSave("生成一个登录页面，要求20行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAICodeAndSaveStream() {
        Flux<String> flux = aiCodeGenerateFacade.generateAICodeAndSaveStream("生成一个登录页面，要求20行", CodeGenTypeEnum.MULTI_FILE);
        Long block = flux.count().block();

    }
}