package com.liud.ainocodegenerator.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.Charset;

@Deprecated
public class CodeFileSaver {

    private static final String FILE_STORAGE_PATH = System.getProperty("user.dir") + "/tmp/code_output";

    // 根据类型存储文件
    public static File saveHtmlCode(HtmlCodeResult htmlCodeResult) {
        String dirPath = generateStoragePath(CodeGenTypeEnum.HTML.getValue());
        saveFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    // 根据类型存储文件
    public static File saveMultiFileCode(MultiFileCodeResult multiFileCodeResult) {
        String dirPath = generateStoragePath(CodeGenTypeEnum.MULTI_FILE.getValue());
        saveFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        saveFile(dirPath, "index.css", multiFileCodeResult.getCssCode());
        saveFile(dirPath, "index.js", multiFileCodeResult.getJsCode());
        return new File(dirPath);
    }

    // 构建唯一路径
    private static String generateStoragePath(String bzyType) {
        String uniquePath = StrUtil.format("{}_{}", bzyType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_STORAGE_PATH + File.separator + uniquePath;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }


    // 保存文件
    private static void saveFile(String dirPath, String fileName, String fileContent) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(fileContent, filePath, Charset.defaultCharset());
    }
}
