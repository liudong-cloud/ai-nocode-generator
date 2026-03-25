package com.liud.ainocodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.core.AICodeGenerateFacade;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.mapper.AppMapper;
import com.liud.ainocodegenerator.model.dto.app.AppQueryRequest;
import com.liud.ainocodegenerator.model.entity.App;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.model.vo.AppVO;
import com.liud.ainocodegenerator.model.vo.UserVO;
import com.liud.ainocodegenerator.service.AppService;
import com.liud.ainocodegenerator.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author liud
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private UserService userService;

    @Resource
    private AICodeGenerateFacade aiCodeGenerateFacade;

    @Override
    public Flux<String> chatToGenCode(Long appId, String prompt, User loginUser) {
        // 1.校验appId是否存在
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId is null");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "loginUser is null");
        // 2.查询应用信息，获取类型
        App app = appMapper.selectOneById(appId);
        String codeGenType = app.getCodeGenType();
        // 3.校验用户是否有权限访问
        Long userId = app.getUserId();
        ThrowUtils.throwIf(!userId.equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        // 4.调用门面方法生成代码（流式）
        return aiCodeGenerateFacade.generateAICodeAndSaveStream(prompt, CodeGenTypeEnum.getEnumByValue(codeGenType), appId);
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息
        Set<Long> userIds = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> idUserMap = userService.listByIds(userIds)
                .stream()
                .map(vo -> userService.getUserVO(vo))
                .collect(Collectors.toMap(UserVO::getId, userVO -> userVO, (e1, e2) -> e1));
        return appList.stream()
                .map(vo -> {
                    AppVO appVO = this.getAppVO(vo);
                    appVO.setUser(idUserMap.get(vo.getUserId()));
                    return appVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));

        return queryWrapper;
    }

}
