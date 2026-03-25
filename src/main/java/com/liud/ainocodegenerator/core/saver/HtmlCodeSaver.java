package com.liud.ainocodegenerator.core.saver;

import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;

public class HtmlCodeSaver extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected File saveFileCode(HtmlCodeResult htmlCodeResult, Long appid) {
        String dirPath = generateStoragePath(appid);
        saveFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    @Override
    protected String getBzyType() {
        return CodeGenTypeEnum.HTML.getValue();
    }
}
