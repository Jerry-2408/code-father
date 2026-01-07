package com.example.codefather.ai.tools;

import cn.hutool.core.io.FileUtil;
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
 * 文件写入工具类
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径") String relativeFileName,
            @P("要写入文件的内容") String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path filePath = Paths.get(relativeFileName);
            if (!filePath.isAbsolute()) {
                // 项目目录相对路径处理，转换为基于appId的项目目录绝对路径
                String projectDirName = "vue_project_" + appId;
                Path projectRootPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                filePath = projectRootPath.resolve(relativeFileName);
            }
            // 创建父目录
            Path parentPath = filePath.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            // 写入文件内容
            Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("写入文件成功：{}", filePath.toAbsolutePath());
            // 返回相对路径，不能返回绝对路径，否则会暴露项目目录结构
            return "文件写入成功：" + relativeFileName;
        } catch (IOException e) {
            String errorMessage = "写入文件失败：" + relativeFileName + "，错误：" + e.getMessage();
            log.error(errorMessage);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFileName = arguments.getStr("relativeFileName");
        String suffix = FileUtil.getSuffix(relativeFileName);
        String content = arguments.getStr("content");
        return String.format("""
                             [工具调用] %s %s
                             ```%s
                             %s
                             """, getDisplayName(), relativeFileName, suffix, content);
    }
}
