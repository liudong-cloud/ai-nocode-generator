package com.liud.ainocodegenerator.config;

import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VueProjectStreamingChatModelConfigTest {

    @Test
    void vueProjectStreamingChatModel_shouldEnableThinkingRoundTrip() {
        VueProjectStreamingChatModelConfig config = new VueProjectStreamingChatModelConfig();
        ReflectionTestUtils.setField(config, "baseUrl", "https://api.example.com");
        ReflectionTestUtils.setField(config, "apiKey", "test-key");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-v4-pro");
        ReflectionTestUtils.setField(config, "maxTokens", 1024);
        ReflectionTestUtils.setField(config, "logRequests", false);
        ReflectionTestUtils.setField(config, "logResponses", false);

        OpenAiStreamingChatModel model = (OpenAiStreamingChatModel) config.vueProjectStreamingChatModel();
        OpenAiChatRequestParameters parameters = model.defaultRequestParameters();

        assertTrue((Boolean) ReflectionTestUtils.getField(model, "returnThinking"));
        assertTrue((Boolean) ReflectionTestUtils.getField(model, "sendThinking"));
        assertEquals(Map.of("thinking", Map.of("type", "enabled")), parameters.customParameters());
    }
}
