package com.liud.ainocodegenerator.langgraph4j.node;

import cn.hutool.core.date.StopWatch;
import com.liud.ainocodegenerator.langgraph4j.ai.ImageCollectionPlanService;
import com.liud.ainocodegenerator.langgraph4j.model.ImageCollectionPlan;
import com.liud.ainocodegenerator.langgraph4j.state.ImageResource;
import com.liud.ainocodegenerator.langgraph4j.state.WorkflowContext;
import com.liud.ainocodegenerator.langgraph4j.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {

    /**
     * 专用线程池：核心线程数 = CPU 核心数，最大线程数 = CPU 核心数 * 2。
     * 使用有界队列防止任务堆积，并通过命名线程方便问题排查。
     */
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService IMAGE_FETCH_EXECUTOR = new ThreadPoolExecutor(
            CPU_CORES,
            CPU_CORES * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "image-fetch-" + r.hashCode());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectedImages = new ArrayList<>();
            // 开头计时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();


            try {
                // 第一步：获取图片收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");
                
                // 第二步：并发执行各种图片收集任务
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 并发执行内容图片搜索
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                            imageSearchTool.searchContentImages(task.query()), IMAGE_FETCH_EXECUTOR));
                    }
                }
                // 并发执行插画图片搜索
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                            illustrationTool.searchIllustrations(task.query()), IMAGE_FETCH_EXECUTOR));
                    }
                }
                // 并发执行架构图生成
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                            diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description()), IMAGE_FETCH_EXECUTOR));
                    }
                }
                // 并发执行Logo生成
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                            logoTool.generateLogos(task.description()), IMAGE_FETCH_EXECUTOR));
                    }
                }
                
                // 等待所有任务完成并收集结果
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("并发图片收集完成，共收集到 {} 张图片", collectedImages.size());
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
            // 结尾停止计时并输出结果
            stopWatch.stop();
            log.info("图片收集总耗时: {} ms", stopWatch.getTotalTimeMillis());
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}
