package com.liud.ainocodegenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liud.ainocodegenerator.ai.tools.*;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AINoCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource(name = "vueProjectStreamingChatModel")
    private StreamingChatModel vueProjectStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AINoCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

//    @Bean
//    AINoCodeGeneratorService generatorService() {
//        return null;
//    }

    public AINoCodeGeneratorService getAINoCodeService(Long appId) {
        return getAINoCodeService(appId, CodeGenTypeEnum.HTML);
    }

    public AINoCodeGeneratorService getAINoCodeService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return serviceCache.get(buildCacheKey(appId, codeGenTypeEnum), k -> createAiNoCodeServer(appId, codeGenTypeEnum));
    }

    private AINoCodeGeneratorService createAiNoCodeServer(Long appId, CodeGenTypeEnum codeGenTypeEnum){
        log.info("创建 AINoCodeGeneratorService, appId: {}, codeGenTypeEnum: {}", appId, codeGenTypeEnum);
        log.info("chatModel: {}, openAiStreamingChatModel: {}, vueProjectStreamingChatModel: {}",
                chatModel, openAiStreamingChatModel, vueProjectStreamingChatModel);
        log.info("redisChatMemoryStore: {}, chatHistoryService: {}", redisChatMemoryStore, chatHistoryService);

        MessageWindowChatMemory windowChatMemory = buildChatMemory(appId, codeGenTypeEnum);
        // 根据
        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> AiServices.builder(AINoCodeGeneratorService.class)
                    .tools(toolManager.getAllTools())
                    .chatModel(chatModel)
                    .chatMemoryProvider(s -> windowChatMemory)
                    .streamingChatModel(vueProjectStreamingChatModel)
                    .build();
            case HTML, MULTI_FILE -> AiServices.builder(AINoCodeGeneratorService.class)
                    .chatMemory(windowChatMemory)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .build();
        };
    }

    private MessageWindowChatMemory buildChatMemory(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(100)
                .build();
        log.info("创建 MessageWindowChatMemory 成功, id: {}", windowChatMemory.id());

        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            log.info("Vue 项目对话跳过数据库文本历史回灌，使用 Redis 原生 memory 作为上下文");
            return windowChatMemory;
        }

        chatHistoryService.loadMemoryFromHistory(appId, windowChatMemory, 20);
        log.info("加载历史记忆完成");
        return windowChatMemory;
    }

    String buildCacheKey(Long appId, CodeGenTypeEnum codeGenTypeEnum){
        return appId + "_" + codeGenTypeEnum.getValue();
    }
}
