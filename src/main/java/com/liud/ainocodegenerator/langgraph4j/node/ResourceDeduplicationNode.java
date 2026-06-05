package com.liud.ainocodegenerator.langgraph4j.node;

import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.langgraph4j.state.ImageResource;
import com.liud.ainocodegenerator.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 资源去重节点
 * <p>
 * 在图片收集完成后、提示词增强前执行，对并发收集到的图片资源进行去重和过滤，
 * 移除 URL 为空的无效资源以及重复的 URL 资源。
 * </p>
 */
@Slf4j
public class ResourceDeduplicationNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 资源去重");

            List<ImageResource> rawImages = context.getImageList();
            if (rawImages == null || rawImages.isEmpty()) {
                log.warn("图片资源列表为空，跳过去重");
                context.setCurrentStep("资源去重");
                return WorkflowContext.saveContext(context);
            }

            List<ImageResource> deduplicated = deduplicate(rawImages);

            int removed = rawImages.size() - deduplicated.size();
            log.info("资源去重完成：原始数量={}, 去重后数量={}, 移除数量={}",
                    rawImages.size(), deduplicated.size(), removed);

            context.setCurrentStep("资源去重");
            context.setImageList(deduplicated);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 对图片资源列表进行去重和过滤
     * <ol>
     *   <li>过滤 URL 为空或空白的无效资源</li>
     *   <li>按 URL 去重（保留首次出现的资源，维持原始顺序）</li>
     * </ol>
     */
    static List<ImageResource> deduplicate(List<ImageResource> images) {
        Set<String> seenUrls = new LinkedHashSet<>();
        List<ImageResource> result = new ArrayList<>();

        for (ImageResource image : images) {
            if (image == null || StrUtil.isBlank(image.getUrl())) {
                log.debug("过滤无效图片资源（URL 为空）");
                continue;
            }
            String normalizedUrl = image.getUrl().trim();
            if (seenUrls.add(normalizedUrl)) {
                result.add(image);
            } else {
                log.debug("过滤重复图片资源：url={}", normalizedUrl);
            }
        }

        return result;
    }
}
