package com.liud.ainocodegenerator.core.handle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liud.ainocodegenerator.ai.tools.BaseTool;
import com.liud.ainocodegenerator.ai.tools.ToolManager;
import com.liud.ainocodegenerator.constant.AppConstant;
import com.liud.ainocodegenerator.core.builder.VueProjectBuilder;
import com.liud.ainocodegenerator.core.diagnostics.ToolArgumentsDiagnostics;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.liud.ainocodegenerator.model.enums.StreamMessageTypeEnum;
import com.liud.ainocodegenerator.model.message.AiResponseMessage;
import com.liud.ainocodegenerator.model.message.StreamMessage;
import com.liud.ainocodegenerator.model.message.ToolExecutedMessage;
import com.liud.ainocodegenerator.model.message.ToolRequestMessage;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AIResponseWithToolJsonHandle {

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ToolManager toolManager;

    public Flux<String> handle(Flux<String> orgFlux, Long appId, User loginUser) {
        StringBuilder aiMessageBuilder = new StringBuilder();
        Map<String, StringBuilder> chunkContentMap = new HashMap<>();
        return orgFlux.map(chunk -> {
                    log.info("chunk: {}", chunk);
                    StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
                    StreamMessageTypeEnum messageTypeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
                    if (messageTypeEnum == null) {
                        log.warn("未知的流式消息类型, chunk={}", chunk);
                        return "";
                    }
                    if (messageTypeEnum == StreamMessageTypeEnum.AI_RESPONSE) {
                        AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                        String data = aiResponseMessage.getData();
                        aiMessageBuilder.append(data);
                        return data;
                    }
                    if (messageTypeEnum == StreamMessageTypeEnum.TOOL_REQUEST) {
                        ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                        String name = toolRequestMessage.getName();
                        String arguments = toolRequestMessage.getArguments();
                        JSONObject argumentObj;
                        try {
                            argumentObj = JSONUtil.parseObj(arguments);
                        } catch (Exception e) {
                            log.error("工具请求参数解析失败, appId: {}, tool: {}, requestId: {}, diagnostics: {}",
                                    appId,
                                    name,
                                    toolRequestMessage.getId(),
                                    ToolArgumentsDiagnostics.summarizeRawArguments(arguments),
                                    e);
                            throw e;
                        }
                        log.info("展示工具请求, appId: {}, tool: {}, requestId: {}, diagnostics: {}",
                                appId,
                                name,
                                toolRequestMessage.getId(),
                                ToolArgumentsDiagnostics.summarizeArgumentObject(argumentObj));
                        BaseTool tool = toolManager.getTool(name);
                        if (tool == null) {
                            log.warn("未找到工具定义, appId: {}, tool: {}, requestId: {}", appId, name, toolRequestMessage.getId());
                            return buildFallbackToolRequestOutput(name);
                        }
                        return StrUtil.nullToDefault(tool.handleToolRequest(argumentObj, chunkContentMap), "");
                    }

                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                    String result = toolExecutedMessage.getResult();
                    String toolName = toolExecutedMessage.getName();
                    JSONObject argumentObj;
                    try {
                        argumentObj = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                    } catch (Exception e) {
                        log.error("工具执行结果参数解析失败, appId: {}, tool: {}, requestId: {}, diagnostics: {}",
                                appId,
                                toolName,
                                toolExecutedMessage.getId(),
                                ToolArgumentsDiagnostics.summarizeRawArguments(toolExecutedMessage.getArguments()),
                                e);
                        String resultInfo = "\n\n[工具-" + toolName + "-执行结果] => " + result + "\n\n";
                        aiMessageBuilder.append(resultInfo);
                        return resultInfo;
                    }
                    BaseTool tool = toolManager.getTool(toolName);
                    String output = tool == null
                            ? buildFallbackToolExecutedOutput(toolName, result)
                            : StrUtil.nullToDefault(tool.handleToolExecuted(result, argumentObj, chunkContentMap), "");
                    aiMessageBuilder.append(output);
                    return output;
                })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    String aiMessage = aiMessageBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 构建代码
                    vueProjectBuilder.buildProjectAsync(AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId);
                })
                .doOnError(e -> {
                    String aiMessage = aiMessageBuilder.toString();
                    if (isStreamClosedException(e)) {
                        log.warn("AI 流式生成被中断, appId: {}", appId, e);
                        if (StrUtil.isNotBlank(aiMessage)) {
                            chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                        }
                        return;
                    }
                    log.error("AI 流式生成异常", e);
                    String errorMessage = StrUtil.blankToDefault(e.getMessage(), "AI 流式生成异常");
                    String historyMessage = StrUtil.isNotBlank(aiMessage)
                            ? aiMessage + "\n\n[系统提示] " + errorMessage
                            : errorMessage;
                    chatHistoryService.addChatMessage(appId, historyMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
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

    private String buildFallbackToolRequestOutput(String toolName) {
        return "\n\n[选择工具] " + toolName + "\n\n";
    }

    private String buildFallbackToolExecutedOutput(String toolName, String result) {
        log.warn("未找到工具定义, tool: {}, 使用兜底执行结果输出", toolName);
        return "\n\n[工具-" + toolName + "-执行结果] => " + result + "\n\n";
    }
}
