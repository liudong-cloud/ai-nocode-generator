package com.liud.ainocodegenerator.core.handle;

import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AIJsonHandleExecutor {

    @Resource
    private AIResponseWithToolJsonHandle aiResponseWithToolJsonHandle;

    @Resource
    private AITextJsonHandle aiTextJsonHandle;


    public Flux<String> handle(Flux<String> orgFlux, CodeGenTypeEnum codeGenTypeEnum, Long appId, User loginUser) {
        return switch (codeGenTypeEnum){
            case VUE_PROJECT -> {
                yield  aiResponseWithToolJsonHandle.handle(orgFlux, appId, loginUser);
            }
             case HTML, MULTI_FILE -> {
                 yield aiTextJsonHandle.handle(orgFlux, appId, loginUser);
            }
            default ->
                throw new BusinessException(ErrorCode.SYSTEM_ERROR ,"不支持的代码生成类型:" + codeGenTypeEnum.getValue());
        };
    }




}
