package com.liud.ainocodegenerator.core;

import com.liud.ainocodegenerator.ai.AINoCodeGeneratorService;
import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

@Service
@Slf4j
public class AICodeGenerateFacade {
    @Resource
    private AINoCodeGeneratorService aiNoCodeGeneratorService;

    /**
     * 统一门面入口
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public File generateAICodeAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlAICodeAndSave(userMessage);
            case MULTI_FILE -> generateMultiFileAICodeAndSave(userMessage);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is invalid");
        };
    }

    /**
     * 统一门面入口 (流式)
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateAICodeAndSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlAICodeAndSaveStream(userMessage);
            case MULTI_FILE -> generateMultiFileAICodeAndSaveStream(userMessage);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is invalid");
        };
    }

    private Flux<String> generateMultiFileAICodeAndSaveStream(String userMessage) {
        Flux<String> flux = aiNoCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        return flux.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String all = stringBuilder.toString();
                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(all);
                        File saveMultiFileCode = CodeFileSaver.saveMultiFileCode(multiFileCodeResult);
                        log.info("保存成功，路径为: {}", saveMultiFileCode.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败", e);
                    }
                });
    }

    private Flux<String> generateHtmlAICodeAndSaveStream(String userMessage) {
        Flux<String> flux = aiNoCodeGeneratorService.generateHtmlCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        return flux.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String all = stringBuilder.toString();
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(all);
                        File htmlCodeFile = CodeFileSaver.saveHtmlCode(htmlCodeResult);
                        log.info("保存成功，路径为: {}", htmlCodeFile.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败", e);
                    }
                });

    }

    private File generateMultiFileAICodeAndSave(String userMessage) {
        MultiFileCodeResult multiFileCodeResult = aiNoCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCode(multiFileCodeResult);
    }

    private File generateHtmlAICodeAndSave(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiNoCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCode(htmlCodeResult);
    }
}
