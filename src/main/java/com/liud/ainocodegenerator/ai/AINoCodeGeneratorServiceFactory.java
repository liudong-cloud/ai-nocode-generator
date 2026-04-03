package com.liud.ainocodegenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AINoCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AINoCodeGeneratorService> serviceCache = Caffeine.newBuilder()
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
        return serviceCache.get(appId, this::createAiNoCodeServer);
    }

    private AINoCodeGeneratorService createAiNoCodeServer(Long appId){
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(20)
                .build();
        // 初始化时候，通过数据库加载历史数据
        chatHistoryService.loadMemoryFromHistory(appId, windowChatMemory, 20);
        return AiServices.builder(AINoCodeGeneratorService.class)
                .chatMemory(windowChatMemory)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }


}
