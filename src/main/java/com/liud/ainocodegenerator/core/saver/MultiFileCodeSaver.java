package com.liud.ainocodegenerator.core.saver;

import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;

public class MultiFileCodeSaver extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected File saveFileCode(MultiFileCodeResult multiFileCodeResult) {
        String dirPath = generateStoragePath();
        saveFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        saveFile(dirPath, "index.css", multiFileCodeResult.getCssCode());
        saveFile(dirPath, "index.js", multiFileCodeResult.getJsCode());
        return new File(dirPath);
    }

    @Override
    protected String getBzyType() {
        return CodeGenTypeEnum.MULTI_FILE.getValue();
    }
}
