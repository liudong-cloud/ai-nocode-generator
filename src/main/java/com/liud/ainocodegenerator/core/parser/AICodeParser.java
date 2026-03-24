package com.liud.ainocodegenerator.core.parser;

import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

public interface AICodeParser<T> {

    T parseCode(String codeContent);
}
