package com.liud.ainocodegenerator.core.handle;

import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class AITextJsonHandle {

    @Resource
    private ChatHistoryService chatHistoryService;

    public Flux<String> handle(Flux<String> flux, Long appId, User loginUser) {
        StringBuilder aiMessageBuilder = new StringBuilder();
        return flux.doOnNext(aiMessageBuilder::append)
                .doOnComplete(() -> {
                    String aiMessage = aiMessageBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(e -> {
                    String aiMessage = aiMessageBuilder.toString();
                    if (isStreamClosedException(e)) {
                        log.warn("AI 文本流式生成被中断, appId: {}", appId, e);
                        if (StrUtil.isNotBlank(aiMessage)) {
                            chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                        }
                        return;
                    }
                    log.error("AI 文本流式生成异常", e);
                    String errorMessage = StrUtil.blankToDefault(e.getMessage(), "AI 文本流式生成异常");
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
}
