package com.example.codefather.ai.tools;

import cn.hutool.json.JSONObject;
import com.example.codefather.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读取工具类
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool {

    @Tool("读取指定路径的文件内容")
    public String readFile(
            @P("文件的相对路径") String relativeFileName,
            @ToolMemoryId Long appId
    ) {
        try {
            Path filePath = Paths.get(relativeFileName);
            // 构建绝对路径
            if (!filePath.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                filePath = projectRoot.resolve(relativeFileName);
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return "错误：文件不存在或不是文件 - " + relativeFileName;
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFileName + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFileName = arguments.getStr("relativeFileName");
        return String.format("[调用结果] %s %s [#调用结果]\n", getDisplayName(), relativeFileName);
    }
}
