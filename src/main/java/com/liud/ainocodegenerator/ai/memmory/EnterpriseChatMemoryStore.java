//package com.liud.ainocodegenerator.ai.memmory;
//
//import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
//import dev.langchain4j.data.message.ChatMessage;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.store.memory.chat.ChatMemoryStore;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class EnterpriseChatMemoryStore implements ChatMemoryStore {
//
//    private final RedisChatMemoryStore redisStore;      // 实际存储
//    private final ChatModel compressionModel;           // 压缩用的模型
//    private final int compressionThreshold;              // 压缩阈值
//
//    @Override
//    public List<ChatMessage> getMessages(Object o) {
//        return List.of();
//    }
//
//    @Override
//    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
//        // 1. 检查是否需要压缩
//        if (needCompression(messages)) {
//            // 2. 调用 LLM 生成摘要
//            String summary = generateSummary(messages);
//            // 3. 创建压缩后的消息列表（摘要 + 最新几条消息）
//            List<ChatMessage> compressed = compressMessages(messages, summary);
//            // 4. 持久化压缩后的消息
//            redisStore.updateMessages(memoryId, compressed);
//        } else {
//            // 直接持久化
//            redisStore.updateMessages(memoryId, messages);
//        }
//    }
//
//    @Override
//    public void deleteMessages(Object o) {
//
//    }
//
//    // 其他方法委托给 redisStore...
//}