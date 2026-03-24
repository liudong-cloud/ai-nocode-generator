package com.liud.ainocodegenerator.core.saver;

import com.liud.ainocodegenerator.ai.model.HtmlCodeResult;
import com.liud.ainocodegenerator.ai.model.MultiFileCodeResult;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {


    private static final HtmlCodeSaver htmlCodeFileSaver = new HtmlCodeSaver();

    private static final MultiFileCodeSaver multiFileCodeFileSaver = new MultiFileCodeSaver();


    public static File saveCodeFile(Object codeContent, CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaver.saveCodeFile((HtmlCodeResult) codeContent, codeGenTypeEnum);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCodeFile((MultiFileCodeResult) codeContent, codeGenTypeEnum);
        };
    }
}
