package com.liud.ainocodegenerator.ai;

import com.liud.ainocodegenerator.ai.tools.BaseTool;
import com.liud.ainocodegenerator.ai.tools.ToolManager;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AINoCodeGeneratorServiceFactoryTest {

    private final AINoCodeGeneratorServiceFactory factory = new AINoCodeGeneratorServiceFactory();
    private final ChatModel chatModel = mock(ChatModel.class);
    private final StreamingChatModel openAiStreamingChatModel = mock(StreamingChatModel.class);
    private final StreamingChatModel vueProjectStreamingChatModel = mock(StreamingChatModel.class);
    private final RedisChatMemoryStore redisChatMemoryStore = mock(RedisChatMemoryStore.class);
    private final ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
    private final ToolManager toolManager = mock(ToolManager.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factory, "chatModel", chatModel);
        ReflectionTestUtils.setField(factory, "openAiStreamingChatModel", openAiStreamingChatModel);
        ReflectionTestUtils.setField(factory, "vueProjectStreamingChatModel", vueProjectStreamingChatModel);
        ReflectionTestUtils.setField(factory, "redisChatMemoryStore", redisChatMemoryStore);
        ReflectionTestUtils.setField(factory, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(factory, "toolManager", toolManager);
        when(toolManager.getAllTools()).thenReturn(new BaseTool[0]);
    }

    @Test
    void getAINoCodeService_shouldSkipDbHistoryReplayForVueProject() {
        factory.getAINoCodeService(1L, CodeGenTypeEnum.VUE_PROJECT);

        verify(chatHistoryService, never()).loadMemoryFromHistory(anyLong(), any(), anyInt());
    }

    @Test
    void getAINoCodeService_shouldReplayDbHistoryForHtml() {
        factory.getAINoCodeService(2L, CodeGenTypeEnum.HTML);

        verify(chatHistoryService).loadMemoryFromHistory(eq(2L), any(), eq(20));
    }
}
