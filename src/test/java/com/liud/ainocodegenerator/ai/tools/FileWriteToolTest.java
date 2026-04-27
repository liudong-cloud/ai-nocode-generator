package com.liud.ainocodegenerator.ai.tools;

import com.liud.ainocodegenerator.constant.AppConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteToolTest {

    private final FileWriteTool fileWriteTool = new FileWriteTool();
    private final Set<Long> appIdsToCleanup = new HashSet<>();

    @AfterEach
    void tearDown() throws IOException {
        for (Long appId : appIdsToCleanup) {
            Path projectDir = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
            if (Files.exists(projectDir)) {
                try (var paths = Files.walk(projectDir)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
        }
        appIdsToCleanup.clear();
    }

    @Test
    void writeFile_shouldWriteCompleteFile() throws IOException {
        long appId = registerAppId();
        String relativePath = "src/main.js";
        String content = "console.log('hello chunkless');";

        String result = fileWriteTool.writeFile(relativePath, content, appId);

        assertTrue(result.contains("文件写入成功"));
        assertEquals(content, Files.readString(resolvePath(appId, relativePath)));
    }

    @Test
    void writeFileChunk_shouldAssembleChunksInOrder() throws IOException {
        long appId = registerAppId();
        String relativePath = "src/pages/Home.vue";

        String first = fileWriteTool.writeFileChunk(relativePath, "<template>\n", 0, false, appId);
        String second = fileWriteTool.writeFileChunk(relativePath, "  <div>首页</div>\n", 1, false, appId);
        String third = fileWriteTool.writeFileChunk(relativePath, "</template>\n", 2, true, appId);

        assertTrue(first.contains("分块文件写入成功"));
        assertTrue(second.contains("分块文件写入成功"));
        assertTrue(third.contains("分块文件写入完成"));
        assertEquals("<template>\n  <div>首页</div>\n</template>\n", Files.readString(resolvePath(appId, relativePath)));
    }

    @Test
    void writeFileChunk_shouldRejectOutOfOrderChunk() throws IOException {
        long appId = registerAppId();
        String relativePath = "src/pages/Articles.vue";

        String first = fileWriteTool.writeFileChunk(relativePath, "chunk-0", 0, false, appId);
        String invalid = fileWriteTool.writeFileChunk(relativePath, "chunk-2", 2, true, appId);

        assertTrue(first.contains("分块文件写入成功"));
        assertTrue(invalid.contains("分块顺序错误"));
        assertEquals("chunk-0", Files.readString(resolvePath(appId, relativePath)));
    }

    private long registerAppId() {
        long appId = System.nanoTime();
        appIdsToCleanup.add(appId);
        return appId;
    }

    private Path resolvePath(long appId, String relativePath) {
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId, relativePath);
    }
}

