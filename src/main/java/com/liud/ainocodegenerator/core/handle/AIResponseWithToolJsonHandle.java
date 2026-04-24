package com.liud.ainocodegenerator.core.handle;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

@Slf4j
@Component
public class AIResponseWithToolJsonHandle {

    @Resource
    private ChatHistoryService chatHistoryService;

    public Flux<String> handle(Flux<String> orgFlux, Long appId, User loginUser) {
        StringBuilder aiMessageBuilder = new StringBuilder();
        return orgFlux.map(chunk -> {
                    log.info("chunk: {}", chunk);
                    StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
                    StreamMessageTypeEnum messageTypeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
                    return switch (messageTypeEnum) {
                        case StreamMessageTypeEnum.AI_RESPONSE:
                            // 转换成aimessage对象
                            AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                            String data = aiResponseMessage.getData();
                            aiMessageBuilder.append(data);
                            yield data;
                        case StreamMessageTypeEnum.TOOL_REQUEST:
                            ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                            String name = toolRequestMessage.getName();
                            String arguments = toolRequestMessage.getArguments();
                            JSONObject argumentObj = JSONUtil.parseObj(arguments);
                            String relativeFilePath = argumentObj.getStr("relativeFilePath");
                            String content = argumentObj.getStr("content");
                            String toolResult = String.format("""
                                    [调用工具] %s，写入文件 %s
                                    ```%s
                                    %s
                                    ```
                                    """, name, relativeFilePath, FileUtil.getSuffix(relativeFilePath), content);
                            String output = String.format("\n\n%s\n\n", toolResult);
                            aiMessageBuilder.append(output);
                            yield output;
                        case StreamMessageTypeEnum.TOOL_EXECUTED:
                            ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                            String result = toolExecutedMessage.getResult();
                            String toolName = toolExecutedMessage.getName();
                            String resultInfo = "\n\n[工具-" + toolName + "-执行结果] => " + result + "\n\n";
                            aiMessageBuilder.append(resultInfo);
                            yield resultInfo;
                        default:
                            log.error("Unknown message type: {}", messageTypeEnum);
                            yield "";
                    };
                })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    String aiMessage = aiMessageBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(e -> {
                    log.error("保存AI生成代码异常", e);
                    chatHistoryService.addChatMessage(appId, e.getMessage(), ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    ;
}
