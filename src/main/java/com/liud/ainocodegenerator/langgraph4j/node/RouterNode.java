package com.liud.ainocodegenerator.langgraph4j.node;

import com.liud.ainocodegenerator.ai.AiCodeGenTypeRoutingService;
import com.liud.ainocodegenerator.langgraph4j.state.WorkflowContext;
import com.liud.ainocodegenerator.langgraph4j.tools.SpringContextUtil;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");
            CodeGenTypeEnum generationType;
            try {
                AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                generationType = aiCodeGenTypeRoutingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("路由决策完成，选择类型: {}", generationType.getText());
            } catch (Exception e) {
                // 路由选择失败，默认用html类型
                log.error("路由决策失败，默认使用HTML类型", e);
                generationType = CodeGenTypeEnum.HTML;
            }
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            log.info("路由决策完成，选择类型: {}", generationType.getText());
            return WorkflowContext.saveContext(context);
        });
    }
}
