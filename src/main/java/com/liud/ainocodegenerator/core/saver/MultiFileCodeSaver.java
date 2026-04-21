package com.liud.ainocodegenerator.core.saver;

import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class MultiFileCodeSaver extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected File saveFileCode(MultiFileCodeResult multiFileCodeResult, Long appid) {
        ThrowUtils.throwIf(multiFileCodeResult == null, new IllegalArgumentException("multiFileCodeResult 不能为空"));
        ThrowUtils.throwIf(StringUtils.isEmpty(multiFileCodeResult.getHtmlCode()), new IllegalArgumentException("htmlCode 不能为空"));
        ThrowUtils.throwIf(StringUtils.isEmpty(multiFileCodeResult.getCssCode()), new IllegalArgumentException("cssCode 不能为空"));
        ThrowUtils.throwIf(StringUtils.isEmpty(multiFileCodeResult.getJsCode()), new IllegalArgumentException("jsCode 不能为空"));
        String dirPath = generateStoragePath(appid);
        saveFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        saveFile(dirPath, "style.css", multiFileCodeResult.getCssCode());
        saveFile(dirPath, "script.js", multiFileCodeResult.getJsCode());
        return new File(dirPath);
    }

    @Override
    protected String getBzyType() {
        return CodeGenTypeEnum.MULTI_FILE.getValue();
    }
}
