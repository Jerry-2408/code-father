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
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具类
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(
            @P("文件的相对路径") String relativeFileName,
            @P("要替换的旧内容") String oldContent,
            @P("要替换的新内容") String newContent,
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
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return "错误，文件不存在或不是文件 - " + relativeFileName;
            }
            // 替换文件内容
            String originalContent = Files.readString(filePath);
            if (!originalContent.contains(oldContent)) {
                return "警告，文件中未找到要替换的内容 - " + relativeFileName;
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "信息：替换后文件内容未发生变化 - " + relativeFileName;
            }
            Files.writeString(filePath, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("修改文件成功：{}", filePath.toAbsolutePath());
            return "文件修改成功：" + relativeFileName;
        } catch (IOException e) {
            String errorMessage = "修改文件失败：" + relativeFileName + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFileName = arguments.getStr("relativeFileName");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        return String.format("""
                [工具调用] %s %s
                
                替换前：
                ```
                %s
                ```
                
                替换后：
                ```
                %s
                ```
                """, getDisplayName(), relativeFileName, oldContent, newContent);
    }

}
