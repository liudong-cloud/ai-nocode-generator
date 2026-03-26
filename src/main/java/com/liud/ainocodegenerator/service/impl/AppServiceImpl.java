package com.liud.ainocodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.core.AICodeGenerateFacade;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.mapper.AppMapper;
import com.liud.ainocodegenerator.model.dto.app.AppQueryRequest;
import com.liud.ainocodegenerator.model.entity.App;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.model.vo.AppVO;
import com.liud.ainocodegenerator.model.vo.UserVO;
import com.liud.ainocodegenerator.service.AppService;
import com.liud.ainocodegenerator.service.ChatHistoryService;
import com.liud.ainocodegenerator.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import static com.liud.ainocodegenerator.constant.AppConstant.CODE_DEPLOY_ROOT_DIR;
import static com.liud.ainocodegenerator.constant.AppConstant.CODE_OUTPUT_ROOT_DIR;

/**
 *  服务层实现。
 *
 * @author liud
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AICodeGenerateFacade aiCodeGenerateFacade;

    @Value("${code.deploy-host:http://localhost:9000}")
    private String deployHost;

    @Override
    public String deploy(Long appId, User user) {
        // 参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId is null");
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "user is null");
        // 判断app是否存在
        App app = appMapper.selectOneById(appId);
        // 判断用户是否有权限
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "app not found");
        ThrowUtils.throwIf(!app.getUserId().equals(user.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        // 判断deployKey是否存在，不存在则生成
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6); // 生成deployKey
        }
        // 构建源文件目录，获取源文件
        String codeGenType = app.getCodeGenType();
        String filePath = StrUtil.format("{}_{}", codeGenType, appId);
        String sourceFilePath = CODE_OUTPUT_ROOT_DIR + File.separator +  filePath;
        File file = new File(sourceFilePath);
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源文件不存在");
        }
        // 构建部署目录，复制源文件
        String deployDir = CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            File depFile = FileUtil.copyContent(file, new File(deployDir), true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败");
        }
        // 修改app信息，部署信息
        App depApp = new App();
        depApp.setId(appId);
        depApp.setDeployKey(deployKey);
        depApp.setDeployedTime(LocalDateTime.now());
        boolean updateRes = this.updateById(depApp);
        ThrowUtils.throwIf(!updateRes, ErrorCode.SYSTEM_ERROR, "部署失败");
        return StrUtil.format("{}/{}/", deployHost, deployKey);
    }

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
        // 4.保存uer消息
        chatHistoryService.addChatMessage(appId, prompt, ChatHistoryMessageTypeEnum.USER.getValue(), userId);
        // 5.调用门面方法生成代码（流式）
        Flux<String> aiCodeRes = aiCodeGenerateFacade.generateAICodeAndSaveStream(prompt, CodeGenTypeEnum.getEnumByValue(codeGenType), appId);
        // 6.保存AI消息
        StringBuilder aiMessageBuilder = new StringBuilder();
        return aiCodeRes.doOnNext(aiMessageBuilder::append)
                .doOnComplete(() -> {
                    String aiMessage = aiMessageBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiMessage, ChatHistoryMessageTypeEnum.AI.getValue(), userId);
                })
                .doOnError(e -> {
                    log.error("保存AI生成代码异常", e);
                    chatHistoryService.addChatMessage(appId, e.getMessage(), ChatHistoryMessageTypeEnum.AI.getValue(), userId);
                });

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        // 校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 删除app的历史消息
        chatHistoryService.deleteByAppId((Long) id);
        return super.removeById(id);
    }
}
