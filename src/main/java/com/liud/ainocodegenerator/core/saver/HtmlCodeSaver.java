package com.liud.ainocodegenerator.core.saver;

import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class HtmlCodeSaver extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected File saveFileCode(HtmlCodeResult htmlCodeResult, Long appid) {
        ThrowUtils.throwIf(htmlCodeResult == null, new IllegalArgumentException("htmlCodeResult 不能为空"));
        ThrowUtils.throwIf(StringUtils.isEmpty(htmlCodeResult.getHtmlCode()), new IllegalArgumentException("htmlCode 不能为空"));
        String dirPath = generateStoragePath(appid);
        saveFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    @Override
    protected String getBzyType() {
        return CodeGenTypeEnum.HTML.getValue();
    }
}
