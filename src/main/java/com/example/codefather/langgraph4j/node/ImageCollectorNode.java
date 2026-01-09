package com.example.codefather.langgraph4j.node;

import com.example.codefather.langgraph4j.ai.ImageCollectionService;
import com.example.codefather.langgraph4j.state.ImageCategoryEnum;
import com.example.codefather.langgraph4j.state.ImageResource;
import com.example.codefather.langgraph4j.state.WorkflowContext;
import com.example.codefather.langgraph4j.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = "";
            log.info("执行节点: 图片收集");

            try {
                // 获取图片收集AI服务实例
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                // 调用图片收集AI服务智能收集图片
                imageListStr = imageCollectionService.collectImages(originalPrompt);
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }

            // 更新状态
            context.setCurrentStep("图片收集");
//            context.setImageList(imageList);
            context.setImageListStr(imageListStr);
//            log.info("图片收集完成，共收集 {} 张图片", imageList.size());
            log.info("图片收集完成: {}", imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }

}
