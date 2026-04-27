package com.liud.ainocodegenerator.core;

import com.liud.ainocodegenerator.ai.AINoCodeGeneratorService;
import com.liud.ainocodegenerator.ai.AINoCodeGeneratorServiceFactory;
import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.core.diagnostics.ToolArgumentsDiagnostics;
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
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

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
            return handleTokenSteam(tokenStream, appid);
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

    private Flux<String> handleTokenSteam(TokenStream tokenStream, Long appId){
        return Flux.create(fluxSink -> {
            AtomicReference<String> lastToolDiagnostics = new AtomicReference<>("<none>");
            fluxSink.onCancel(() -> log.info("Vue 项目代码生成流被下游取消"));
            fluxSink.onDispose(() -> log.debug("Vue 项目代码生成流已释放"));
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        emitNextSafely(fluxSink, Json.toJson(aiResponseMessage));
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
                        String toolDiagnostics = ToolArgumentsDiagnostics.summarizeRawArguments(beforeToolExecution.request().arguments());
                        lastToolDiagnostics.set("tool=" + beforeToolExecution.request().name()
                                + ", requestId=" + beforeToolExecution.request().id()
                                + ", " + toolDiagnostics);
                        log.info("准备执行工具, appId: {}, tool: {}, requestId: {}, diagnostics: {}",
                                appId,
                                beforeToolExecution.request().name(),
                                beforeToolExecution.request().id(),
                                toolDiagnostics);
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(beforeToolExecution.request().id(), beforeToolExecution.request().name(), beforeToolExecution.request().arguments());
                        emitNextSafely(fluxSink, Json.toJson(toolRequestMessage));
                    })
                    // This will be invoked right after a tool is executed. ToolExecution contains ToolExecutionRequest and tool execution result.
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        log.info("工具执行完成, appId: {}, tool: {}, requestId: {}, resultPreview: {}",
                                appId,
                                toolExecution.request().name(),
                                toolExecution.request().id(),
                                ToolArgumentsDiagnostics.summarizeText(toolExecution.result()));
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution.request().id(), toolExecution.request().name(), toolExecution.request().arguments(), toolExecution.result());
                        emitNextSafely(fluxSink, Json.toJson(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        log.info("Vue 项目代码生成流完成");
                        if (!fluxSink.isCancelled()) {
                            fluxSink.complete();
                        }
                    })
                    .onError(error -> {
                        if (isStreamClosedException(error)) {
                            log.warn("Vue 项目代码生成流被关闭: {}, appId: {}, lastToolDiagnostics: {}",
                                    findThrowableMessage(error), appId, lastToolDiagnostics.get(), error);
                        } else {
                            log.error("Vue 项目代码生成流异常, appId: {}, lastToolDiagnostics: {}", appId, lastToolDiagnostics.get(), error);
                        }
                        if (!fluxSink.isCancelled()) {
                            fluxSink.error(error);
                        }
                    })
                    .start();
        });
    }

    private void emitNextSafely(FluxSink<String> fluxSink, String payload) {
        if (!fluxSink.isCancelled()) {
            fluxSink.next(payload);
        }
    }

    private boolean isStreamClosedException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("closed")
                        || lowerMessage.contains("broken pipe")
                        || lowerMessage.contains("connection reset")
                        || lowerMessage.contains("cancel")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String findThrowableMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return "unknown";
    }
}
