package com.example.codefather.ai.tools;

import com.example.codefather.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径") String relativeFilePath,
            @P("要写入文件的内容") String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // 项目目录相对路径处理，转换为基于appId的项目目录绝对路径
                String projectDirName = "vue_project_" + appId;
                Path projectRootPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRootPath.resolve(relativeFilePath);
            }
            // 创建父目录
            Path parentPath = path.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("写入文件成功：{}", path.toAbsolutePath());
            // 返回相对路径，不能返回绝对路径，否则会暴露项目目录结构
            return "文件写入成功：" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "写入文件失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage);
            return errorMessage;
        }
    }
}
