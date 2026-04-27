package com.liud.ainocodegenerator.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class VueProjectStreamingChatModelConfig {

    @Value("${langchain4j.open-ai.streaming-chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.streaming-chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.streaming-chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:384000}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.streaming-chat-model.log-requests:true}")
    private Boolean logRequests;

    @Value("${langchain4j.open-ai.streaming-chat-model.log-responses:true}")
    private Boolean logResponses;

    /**
     * Vue 项目生成专用流式模型：显式关闭 thinking / reasoning 回传，避免多轮对话中要求带回 reasoning_content。
     */
    @Bean("vueProjectStreamingChatModel")
    public StreamingChatModel vueProjectStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .returnThinking(false)
                .sendThinking(false)
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .build();
    }
}
