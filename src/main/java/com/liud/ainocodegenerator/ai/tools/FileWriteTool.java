package com.liud.ainocodegenerator.ai.tools;

import cn.hutool.json.JSONObject;
import com.liud.ainocodegenerator.constant.AppConstant;
import com.liud.ainocodegenerator.core.diagnostics.ToolArgumentsDiagnostics;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    private static final ConcurrentMap<String, Integer> FILE_CHUNK_INDEX_CACHE = new ConcurrentHashMap<>();

    @Tool("文件写入工具")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        log.info("接收到文件写入工具调用, appId: {}, diagnostics: {}",
                appId,
                ToolArgumentsDiagnostics.summarizeWriteFileInvocation(relativeFilePath, content));
        try {
            Path path = resolveFilePath(relativeFilePath, appId);
            createParentDirectories(path);
            Files.writeString(path,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            clearChunkState(relativeFilePath, appId);
            log.info("成功写入文件: {}, appId: {}, diagnostics: {}",
                    path.toAbsolutePath(),
                    appId,
                    ToolArgumentsDiagnostics.summarizeWriteFileInvocation(relativeFilePath, content));
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Tool("分块文件写入工具")
    public String writeFileChunk(
            @P("文件的相对路径") String relativeFilePath,
            @P("当前分块的内容，必须是按顺序写入的源码片段") String chunkContent,
            @P("当前分块的序号，从 0 开始递增") Integer chunkIndex,
            @P("当前分块是否为当前文件的最后一个分块") Boolean lastChunk,
            @ToolMemoryId Long appId
    ) {
        log.info("接收到分块文件写入工具调用, appId: {}, diagnostics: {}",
                appId,
                ToolArgumentsDiagnostics.summarizeWriteFileChunkInvocation(relativeFilePath, chunkContent, chunkIndex, lastChunk));
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return "分块文件写入失败: relativeFilePath 不能为空";
        }
        if (chunkContent == null) {
            return "分块文件写入失败: chunkContent 不能为空";
        }
        if (chunkIndex == null || chunkIndex < 0) {
            return "分块文件写入失败: chunkIndex 必须是大于等于 0 的整数";
        }
        boolean isLastChunk = Boolean.TRUE.equals(lastChunk);
        String stateKey = buildStateKey(relativeFilePath, appId);
        try {
            Path path = resolveFilePath(relativeFilePath, appId);
            createParentDirectories(path);
            Integer previousChunkIndex = FILE_CHUNK_INDEX_CACHE.get(stateKey);
            if (chunkIndex == 0) {
                Files.writeString(path,
                        chunkContent,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
            } else {
                if (previousChunkIndex == null) {
                    return "分块文件写入失败: 缺少首个分块，relativeFilePath=" + relativeFilePath;
                }
                if (chunkIndex != previousChunkIndex + 1) {
                    return "分块文件写入失败: 分块顺序错误，expected=" + (previousChunkIndex + 1) + ", actual=" + chunkIndex + ", relativeFilePath=" + relativeFilePath;
                }
                Files.writeString(path,
                        chunkContent,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.WRITE);
            }
            if (isLastChunk) {
                clearChunkState(relativeFilePath, appId);
                String result = "分块文件写入完成: " + relativeFilePath + "，最后分块序号=" + chunkIndex;
                log.info(result + ", appId: {}", appId);
                return result;
            }
            FILE_CHUNK_INDEX_CACHE.put(stateKey, chunkIndex);
            String result = "分块文件写入成功: " + relativeFilePath + "，分块序号=" + chunkIndex;
            log.info(result + ", appId: {}", appId);
            return result;
        } catch (IOException e) {
            String errorMessage = "分块文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private Path resolveFilePath(String relativeFilePath, Long appId) {
        Path path = Paths.get(relativeFilePath);
        if (!path.isAbsolute()) {
            String projectDirName = "vue_project_" + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
            path = projectRoot.resolve(relativeFilePath);
        }
        return path;
    }

    private void createParentDirectories(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
    }

    private void clearChunkState(String relativeFilePath, Long appId) {
        FILE_CHUNK_INDEX_CACHE.remove(buildStateKey(relativeFilePath, appId));
    }

    private String buildStateKey(String relativeFilePath, Long appId) {
        return appId + ":" + relativeFilePath;
    }

    // 核心方法不变，此处省略

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String[] getSupportedToolNames() {
        return new String[]{"writeFile", "writeFileChunk"};
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String content = arguments.getStr("content");
        return formatFileCodeOutput(relativeFilePath, content);
    }

    @Override
    public String handleToolRequest(JSONObject arguments, java.util.Map<String, StringBuilder> chunkContentMap) {
        if (isWriteFileChunkRequest(arguments)) {
            bufferChunkContent(chunkContentMap, arguments);
            return "";
        }
        return super.handleToolRequest(arguments, chunkContentMap);
    }

    @Override
    public String handleToolExecuted(String result, JSONObject arguments, java.util.Map<String, StringBuilder> chunkContentMap) {
        if (isWriteFileChunkRequest(arguments)) {
            String relativeFilePath = arguments.getStr("relativeFilePath");
            Boolean lastChunk = arguments.getBool("lastChunk");
            if (Boolean.TRUE.equals(lastChunk) && isToolExecutionSuccess(result)) {
                StringBuilder fileContentBuilder = chunkContentMap.remove(relativeFilePath);
                String fullContent = fileContentBuilder == null ? arguments.getStr("chunkContent", "") : fileContentBuilder.toString();
                return formatFileCodeOutput(relativeFilePath, fullContent);
            }
            if (!isToolExecutionSuccess(result)) {
                return formatGenericToolExecutedOutput("writeFileChunk", result);
            }
            return "";
        }
        return super.handleToolExecuted(result, arguments, chunkContentMap);
    }

    private boolean isWriteFileChunkRequest(JSONObject arguments) {
        return arguments.containsKey("chunkContent") || arguments.containsKey("chunkIndex") || arguments.containsKey("lastChunk");
    }

    private void bufferChunkContent(java.util.Map<String, StringBuilder> chunkContentMap, JSONObject argumentObj) {
        String relativeFilePath = argumentObj.getStr("relativeFilePath");
        String chunkContent = argumentObj.getStr("chunkContent", "");
        Integer chunkIndex = argumentObj.getInt("chunkIndex", 0);
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return;
        }
        if (chunkIndex != null && chunkIndex == 0) {
            chunkContentMap.put(relativeFilePath, new StringBuilder(chunkContent));
            return;
        }
        chunkContentMap.computeIfAbsent(relativeFilePath, key -> new StringBuilder()).append(chunkContent);
    }

}
