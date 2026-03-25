package com.liud.ainocodegenerator.service;

import com.liud.ainocodegenerator.model.dto.app.AppQueryRequest;
import com.liud.ainocodegenerator.model.entity.App;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 服务层。
 *
 * @author liud
 */
public interface AppService extends IService<App> {

    /**
     * 部署应用
     *
     * @param appId 应用id
     * @param user 用户
     * @return 部署结果
     */
    String deploy(Long appId, User user);

    /**
     * 聊天生成代码
     *
     * @param appId
     * @param prompt
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String prompt, User loginUser);

    /**
     * 获取应用视图对象
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图对象列表
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param appQueryRequest 查询请求
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
