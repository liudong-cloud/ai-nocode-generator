package com.liud.ainocodegenerator.core;

import com.liud.ainocodegenerator.ai.AINoCodeGeneratorService;
import com.liud.ainocodegenerator.ai.AINoCodeGeneratorServiceFactory;
import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.core.parser.CodeParserExecutor;
import com.liud.ainocodegenerator.core.saver.CodeFileSaverExecutor;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.model.message.AiResponseMessage;
import com.liud.ainocodegenerator.model.message.ToolExecutedMessage;
import com.liud.ainocodegenerator.model.message.ToolRequestMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

@Service
@Slf4j
public class AICodeGenerateFacade {
    @Resource
    private AINoCodeGeneratorServiceFactory aiNoCodeGeneratorServiceFactory;

    /**
     * 统一门面入口
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public File generateAICodeAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appid) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is null");
        }
        AINoCodeGeneratorService aiNoCodeGeneratorService = aiNoCodeGeneratorServiceFactory.getAINoCodeService(appid);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiNoCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(htmlCodeResult, codeGenTypeEnum, appid);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiNoCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.saveCodeFile(multiFileCodeResult, codeGenTypeEnum, appid);
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
    public Flux<String> generateAICodeAndSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appid) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> processCodeStream(userMessage, CodeGenTypeEnum.HTML, appid);
            case MULTI_FILE -> processCodeStream(userMessage, CodeGenTypeEnum.MULTI_FILE, appid);
            case VUE_PROJECT -> processCodeStream(userMessage, CodeGenTypeEnum.VUE_PROJECT, appid);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is invalid");
        };
    }

    private Flux<String> processCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appid) {
        AINoCodeGeneratorService aiNoCodeGeneratorService = aiNoCodeGeneratorServiceFactory.getAINoCodeService(appid, codeGenTypeEnum);
        if (codeGenTypeEnum.equals(CodeGenTypeEnum.VUE_PROJECT)) {
            TokenStream tokenStream = aiNoCodeGeneratorService.generateVueProjectCodeStream(appid, userMessage);
            return handleTokenSteam(tokenStream);
        }
        Flux<String> flux = aiNoCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        return flux.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String all = stringBuilder.toString();
                        Object codeParser = CodeParserExecutor.codeParser(all, codeGenTypeEnum);
                        File saveFile = CodeFileSaverExecutor.saveCodeFile(codeParser, codeGenTypeEnum, appid);
                        log.info("保存成功，路径为: {}", saveFile.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败", e);
                    }
                });
    }

    private Flux<String> handleTokenSteam(TokenStream tokenStream){
        return Flux.create(fluxSink -> {
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        fluxSink.next(Json.toJson(aiResponseMessage));
                    })
                    // .onPartialThinking((PartialThinking partialThinking) -> fluxSink.next(partialThinking.toString()))
                    // .onRetrieved((List<Content> contents) -> fluxSink.next(contents.toString()))
                    // .onIntermediateResponse((ChatResponse intermediateResponse) -> fluxSink.next(intermediateResponse.toString()))
                    // This will be invoked every time a new partial tool call (usually containing a single token of the tool's arguments) is available.
//                    .onPartialToolCall((PartialToolCall partialToolCall) -> {
//                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolCall.id(), partialToolCall.name(), partialToolCall.partialArguments());
//                        fluxSink.next(Json.toJson(toolRequestMessage));
//                    })
                    // This will be invoked right before a tool is executed. BeforeToolExecution contains ToolExecutionRequest (e.g. tool name, tool arguments, etc.)
                    .beforeToolExecution((BeforeToolExecution beforeToolExecution) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(beforeToolExecution.request().id(), beforeToolExecution.request().name(), beforeToolExecution.request().arguments());
                        fluxSink.next(Json.toJson(toolRequestMessage));
                    })
                    // This will be invoked right after a tool is executed. ToolExecution contains ToolExecutionRequest and tool execution result.
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution.request().id(), toolExecution.request().name(), toolExecution.request().arguments(), toolExecution.result());
                        fluxSink.next(Json.toJson(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> fluxSink.complete())
                    .onError(fluxSink::error)
                    .start();
        });
    }
}
