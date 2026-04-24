package com.liud.ainocodegenerator.model.message;

import com.liud.ainocodegenerator.model.enums.StreamMessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolRequestMessage extends StreamMessage{

    private String id;
    private String name;
    private String arguments;

    public ToolRequestMessage(String id, String name, String arguments) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }
}
