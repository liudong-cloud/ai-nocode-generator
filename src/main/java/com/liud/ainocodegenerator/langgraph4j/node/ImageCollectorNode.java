package com.liud.ainocodegenerator.langgraph4j.node;

import com.liud.ainocodegenerator.langgraph4j.ai.ImageCollectionService;
import com.liud.ainocodegenerator.langgraph4j.state.WorkflowContext;
import com.liud.ainocodegenerator.langgraph4j.tools.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 图片收集");
            String imagesStr = null;

            try {
                // 获取AI图片服务
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                // 图片收集
                imagesStr = imageCollectionService.collectImages(context.getOriginalPrompt());
            } catch (Exception e) {
                log.error("图片收集失败", e);
            }

            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageListStr(imagesStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
