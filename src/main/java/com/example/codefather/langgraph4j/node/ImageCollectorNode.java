package com.example.codefather.langgraph4j.node;

import com.example.codefather.langgraph4j.ai.ImageCollectionPlanService;
import com.example.codefather.langgraph4j.ai.ImageCollectionService;
import com.example.codefather.langgraph4j.model.ImageCollectionPlan;
import com.example.codefather.langgraph4j.model.ImageResource;
import com.example.codefather.langgraph4j.state.WorkflowContext;
import com.example.codefather.langgraph4j.tools.ImageSearchTool;
import com.example.codefather.langgraph4j.tools.LogoGeneratorTool;
import com.example.codefather.langgraph4j.tools.MermaidDiagramTool;
import com.example.codefather.langgraph4j.tools.UndrawIllustrationTool;
import com.example.codefather.langgraph4j.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
//            String imageListStr = "";
            List<ImageResource> collectedImages = new ArrayList<>();
            log.info("执行节点: 图片收集");
            try {
                // 获取图片收集AI服务实例
//                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                // 调用图片收集AI服务智能收集图片
//                imageListStr = imageCollectionService.collectImages(originalPrompt);
                // 1. 获取图片收集计划
                ImageCollectionPlanService imageCollectionPlanService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = imageCollectionPlanService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");
                // 2. 并发执行各种图片收集任务
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 并发搜索内容图片
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                                () -> imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 并发搜索插画图片
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool undrawIllustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                                () -> undrawIllustrationTool.searchIllustrations(task.query())));
                    }
                }
                // 并发搜索架构图
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                                () -> diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 并发搜索Logo图片
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                                () -> logoTool.generateLogos(task.description())));
                    }
                }
                // 3.等待所有任务完成
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 4. 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> imageResources = future.get();
                    if (imageResources != null) {
                        collectedImages.addAll(imageResources);
                    }
                }
                log.info("图片收集完成，共收集 {} 张图片", collectedImages.size());
//            log.info("图片收集完成: {}", imageListStr);
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
//            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }

}
