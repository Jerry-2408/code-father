package com.example.codefather.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理类
 */
@Slf4j
@Component
public class ToolManager {

    private final Map<String, BaseTool> toolMap = new HashMap<>();

    @Resource
    private BaseTool[] tools; // 注意，注入多个实例需要用数组[]，否则langchain4j的AI Service无法正确导入工具

    /**
     * 初始化工具
     */
    @PostConstruct // 完成了依赖注入（即所有的 @Autowired 属性都已赋值）之后，会自动调用标注了 @PostConstruct 的方法
    // 学习：当 Spring 容器准备关闭，并准备销毁这个 Bean 实例之前，会自动调用标注了 @PreDestroy 的方法
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("注册工具：{} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具注册完成，共{}个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具实例
     * @param toolName 工具名称
     * @return 工具实例
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 获取所有工具实例
     * @return 工具实例数组
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}
