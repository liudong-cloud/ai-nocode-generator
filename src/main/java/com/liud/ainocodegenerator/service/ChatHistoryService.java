package com.liud.ainocodegenerator.service;

import com.liud.ainocodegenerator.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.liud.ainocodegenerator.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 *  服务层。
 *
 * @author liud
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    boolean deleteByAppId(Long id);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
}
