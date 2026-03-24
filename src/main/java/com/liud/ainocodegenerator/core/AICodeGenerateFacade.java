package com.liud.ainocodegenerator.core;

import com.liud.ainocodegenerator.ai.AINoCodeGeneratorService;
import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.core.parser.CodeParserExecutor;
import com.liud.ainocodegenerator.core.saver.CodeFileSaverExecutor;
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
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiNoCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(htmlCodeResult, codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiNoCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(multiFileCodeResult, codeGenTypeEnum);
            }
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
            case HTML -> processCodeStream(userMessage, CodeGenTypeEnum.HTML);
            case MULTI_FILE -> processCodeStream(userMessage, CodeGenTypeEnum.MULTI_FILE);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is invalid");
        };
    }

    private Flux<String> processCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        Flux<String> flux = aiNoCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        return flux.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String all = stringBuilder.toString();
                        Object codeParser = CodeParserExecutor.codeParser(all, codeGenTypeEnum);
                        File saveFile = CodeFileSaverExecutor.saveCodeFile(codeParser, codeGenTypeEnum);
                        log.info("保存成功，路径为: {}", saveFile.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败", e);
                    }
                });
    }
}
