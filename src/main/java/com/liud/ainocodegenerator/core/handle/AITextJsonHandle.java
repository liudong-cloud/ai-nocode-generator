package com.liud.ainocodegenerator.core.handle;

import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
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
                    log.error("保存AI生成代码异常", e);
                    chatHistoryService.addChatMessage(appId, e.getMessage(), ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}
