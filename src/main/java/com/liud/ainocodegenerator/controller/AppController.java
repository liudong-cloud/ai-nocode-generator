package com.liud.ainocodegenerator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.annotation.AuthCheck;
import com.liud.ainocodegenerator.common.BaseResponse;
import com.liud.ainocodegenerator.common.DeleteRequest;
import com.liud.ainocodegenerator.common.ResultUtils;
import com.liud.ainocodegenerator.constant.AppConstant;
import com.liud.ainocodegenerator.constant.UserConstant;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.exception.ThrowUtils;
import com.liud.ainocodegenerator.model.dto.app.AppAddRequest;
import com.liud.ainocodegenerator.model.dto.app.AppDeployRequest;
import com.liud.ainocodegenerator.model.dto.app.AppQueryRequest;
import com.liud.ainocodegenerator.model.dto.app.AppUpdateRequest;
import com.liud.ainocodegenerator.model.entity.App;
import com.liud.ainocodegenerator.model.entity.User;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;
import com.liud.ainocodegenerator.model.vo.AppVO;
import com.liud.ainocodegenerator.service.AppService;
import com.liud.ainocodegenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import dev.langchain4j.internal.Json;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.Param;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 *  控制层。
 *
 * @author liud
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @PostMapping("/deploy")
    public BaseResponse<String> deploy(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest httpServletRequest){
        // 校验参数
        ThrowUtils.throwIf(appDeployRequest == null || appDeployRequest.getAppId() == null, ErrorCode.PARAMS_ERROR);
        // 获取用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 部署
        String deploy = appService.deploy(appDeployRequest.getAppId(), loginUser);
        return ResultUtils.success(deploy);

    }

    /**
     * 对话生成代码
     *
     * @param appId 应用 id
     * @param prompt 输入的提示
     * @param httpServletRequest HTTP 请求
     * @return 给到前端的信息
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@Param("appId") Long appId, @Param("prompt") String prompt, HttpServletRequest httpServletRequest) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "appId参数有误");
        ThrowUtils.throwIf(StrUtil.isBlank(prompt), ErrorCode.PARAMS_ERROR, "输入的提示参数不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        return appService.chatToGenCode(appId, prompt, loginUser).map(content -> {
            HashMap<String, String> d = MapUtil.of("d", content);
            return ServerSentEvent.builder(Json.toJson(d)).build();
        }).concatWith(Flux.just(ServerSentEvent.<String>builder().event("end").data("").build()));
    }

    /**
     * 创建应用（用户）
     *
     * @param appAddRequest 应用创建请求
     * @param request       请求对象
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验应用数据
        String initPrompt = appAddRequest.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 Prompt 不能为空");
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        app.setUserId(loginUser.getId());
        app.setInitPrompt(initPrompt);
        // 应用名称暂为initPrompt截取前12个字符
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 暂时定位MultiFile
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 根据 id 修改自己的应用（用户）
     *
     * @param appUpdateRequest 应用更新请求
     * @param request          请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 查询应用是否存在
        App oldApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限：只能修改自己的应用
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 更新应用（仅支持修改应用名称）
        App app = new App();
        BeanUtil.copyProperties(appUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 删除自己的应用（用户）
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 查询应用是否存在
        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限：只能删除自己的应用 或者管理员也可以删除应用
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情（用户）
     *
     * @param id      应用 id
     * @param request 请求对象
     * @return 应用视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询应用是否存在
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（用户）
     *
     * @param appQueryRequest 查询请求参数
     * @param request         请求对象
     * @return 分页结果
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 设置查询条件为当前用户
        appQueryRequest.setUserId(loginUser.getId());
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多20条数据");
        long pageNum = appQueryRequest.getPageNum();
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        // 转换为 VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选的应用列表（用户）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多20条数据");
        long pageNum = appQueryRequest.getPageNum();
        // 设置优先级为精选
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        // 转换为 VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 删除任意应用（管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 判断是否存在
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 更新任意应用（管理员）
     *
     * @param appUpdateRequest 应用更新请求
     * @return 是否成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppUpdateRequest appUpdateRequest) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appUpdateRequest, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询应用列表（管理员）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        long pageNum = appQueryRequest.getPageNum();
        if (pageSize < 1) {
            pageSize = 10;
        }
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        // 转换为 VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 查看应用详情（管理员）
     *
     * @param id 应用 id
     * @return 应用视图对象
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppByIdAdmin(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }
}
