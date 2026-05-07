package com.liud.ainocodegenerator.core.handle;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liud.ainocodegenerator.ai.tools.BaseTool;
import com.liud.ainocodegenerator.ai.tools.ToolManager;
import com.liud.ainocodegenerator.core.builder.VueProjectBuilder;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.message.ToolExecutedMessage;
import com.liud.ainocodegenerator.model.message.ToolRequestMessage;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AIResponseWithToolJsonHandleTest {

    private final AIResponseWithToolJsonHandle handle = new AIResponseWithToolJsonHandle();
    private final ChatHistoryService chatHistoryService = Mockito.mock(ChatHistoryService.class);
    private final VueProjectBuilder vueProjectBuilder = Mockito.mock(VueProjectBuilder.class);

    @BeforeEach
    void setUp() {
        when(chatHistoryService.addChatMessage(anyLong(), anyString(), anyString(), anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(handle, "chatHistoryService", chatHistoryService);
        ReflectionTestUtils.setField(handle, "vueProjectBuilder", vueProjectBuilder);
    }

    @Test
    void handle_shouldDelegateToolRequestAndToolExecutedOutputToToolManager() {
        ToolManager toolManager = buildToolManager(new DelegatingTool());
        ReflectionTestUtils.setField(handle, "toolManager", toolManager);
        String arguments = JSONUtil.createObj()
                .set("relativeFilePath", "src/App.vue")
                .toString();

        List<String> outputs = handle.handle(Flux.just(
                        JSONUtil.toJsonStr(new ToolRequestMessage("req-1", "delegatingTool", arguments)),
                        JSONUtil.toJsonStr(new ToolExecutedMessage("req-1", "delegatingTool", arguments, "执行成功"))
                ), 1L, User.builder().id(99L).build())
                .collectList()
                .block();

        assertEquals(List.of(
                "\n\n[选择工具] 自定义工具\n\n",
                "[工具调用] 自定义工具 src/App.vue"
        ), outputs);
    }

    @Test
    void handle_shouldFallbackToGenericRequestAndExecutedOutputWhenToolIsMissing() {
        ToolManager toolManager = buildToolManager(new DelegatingTool());
        ReflectionTestUtils.setField(handle, "toolManager", toolManager);
        String arguments = new JSONObject().toString();

        List<String> outputs = handle.handle(Flux.just(
                        JSONUtil.toJsonStr(new ToolRequestMessage("req-2", "unknownTool", arguments)),
                        JSONUtil.toJsonStr(new ToolExecutedMessage("req-2", "unknownTool", arguments, "执行成功"))
                ), 2L, User.builder().id(100L).build())
                .collectList()
                .block();

        assertEquals(List.of(
                "\n\n[选择工具] unknownTool\n\n",
                "\n\n[工具-unknownTool-执行结果] => 执行成功\n\n"
        ), outputs);
    }

    private ToolManager buildToolManager(BaseTool... tools) {
        ToolManager toolManager = new ToolManager();
        ReflectionTestUtils.setField(toolManager, "tools", tools);
        toolManager.initTools();
        return toolManager;
    }

    private static class DelegatingTool extends BaseTool {

        @Override
        public String getToolName() {
            return "delegatingTool";
        }

        @Override
        public String getDisplayName() {
            return "自定义工具";
        }

        @Override
        public String generateToolExecutedResult(JSONObject arguments) {
            return "[工具调用] 自定义工具 " + arguments.getStr("relativeFilePath");
        }
    }
}
