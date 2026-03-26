package com.liud.ainocodegenerator.controller;

import com.liud.ainocodegenerator.annotation.AuthCheck;
import com.liud.ainocodegenerator.common.BaseResponse;
import com.liud.ainocodegenerator.common.ResultUtils;
import com.liud.ainocodegenerator.constant.UserConstant;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.liud.ainocodegenerator.model.entity.ChatHistory;
import com.liud.ainocodegenerator.service.ChatHistoryService;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  控制层。
 *
 * @author liud
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> adminListAppChatHistory(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
                                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = chatHistoryQueryRequest.getAppId();
        int pageSize = chatHistoryQueryRequest.getPageSize();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }


}
