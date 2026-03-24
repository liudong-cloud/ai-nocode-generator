package com.liud.ainocodegenerator.core.parser;

import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {

    public static Object codeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum) {
            case HTML -> new HtmlCodeParser().parseCode(codeContent);
            case MULTI_FILE -> new MultiFileCodeParser().parseCode(codeContent);
        };
    }

}
