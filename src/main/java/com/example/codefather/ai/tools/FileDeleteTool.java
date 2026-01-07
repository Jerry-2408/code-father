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
 * 删除文件工具类
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    @Tool("删除指定路径的文件")
    public String deleteFile(
            @P("文件的相对路径") String relativeFileName,
            @ToolMemoryId Long appId
    ) {
        try {
            Path filePath = Paths.get(relativeFileName);
            // 构建绝对路径
            if (!filePath.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRootPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                filePath = projectRootPath.resolve(relativeFileName);
            }
            if (!Files.exists(filePath)) {
                return "警告，文件不存在，无需删除 - " + relativeFileName;
            }
            if (!Files.isRegularFile(filePath)) {
                return "错误，指定路径不是文件，无法删除 - " + relativeFileName;
            }
            // 安全检查，避免删除重要文件
            String fileName = filePath.getFileName().toString();
            if (isImportantFile(fileName)) {
                return "错误，文件是重要文件，不允许删除 - " + relativeFileName;
            }
            // 删除文件
            Files.delete(filePath);
            log.info("删除文件成功：{}", filePath.toAbsolutePath());
            return "文件删除成功：" + relativeFileName;
        } catch (IOException e) {
            String errorMessage = "删除文件失败：" + relativeFileName + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否是重要文件，不允许删除
     */
    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (important.equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "删除文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFileName = arguments.getStr("relativeFileName");
        return String.format("[工具调用] %s %s", getDisplayName(), relativeFileName);
    }
}
