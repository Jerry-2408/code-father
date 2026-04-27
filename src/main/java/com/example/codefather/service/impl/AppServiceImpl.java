package com.example.codefather.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.codefather.ai.AiCodeGenTypeRoutingService;
import com.example.codefather.ai.AiCodeGenTypeRoutingServiceFactory;
import com.example.codefather.constant.AppConstant;
import com.example.codefather.constant.UserConstant;
import com.example.codefather.core.AiCodeGeneratorFacade;
import com.example.codefather.core.handler.StreamHandlerExecutor;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.model.dto.app.AppAddDTO;
import com.example.codefather.model.dto.app.AppQueryDTO;
import com.example.codefather.model.entity.AppDeployTask;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.enums.AppDeployTaskStatusEnum;
import com.example.codefather.model.enums.ChatHistoryMessageTypeEnum;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import com.example.codefather.model.vo.app.AppDeployTaskVO;
import com.example.codefather.model.vo.app.AppVO;
import com.example.codefather.model.vo.user.UserVO;
import com.example.codefather.monitor.MonitorContext;
import com.example.codefather.monitor.MonitorContextHolder;
import com.example.codefather.mq.producer.AppDeployTaskProducer;
import com.example.codefather.service.*;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.codefather.model.entity.App;
import com.example.codefather.mapper.AppMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.codec.ServerSentEvent;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Lazy
    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private AppDeployTaskService appDeployTaskService;

    @Resource
    private AppDeployTaskProducer appDeployTaskProducer;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Flux<ServerSentEvent<String>> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 用户校验
        if (!loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型并校验
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        // 5. 添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        chatHistoryOriginalService.addOriginalChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 添加AI监控上下文
        MonitorContext monitorContext = MonitorContext.builder()
                .appId(appId.toString())
                .userId(loginUser.getId().toString())
                .build();
        MonitorContextHolder.setContext(monitorContext);
        // 7. 调用 AI 生成代码，使用方法参数的message而不是app中的Init Prompt，方便后续复用该接口
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenType, appId);
        // 8. 流结束时清除AI监控上下文
        codeStream = codeStream.doFinally(signalType -> MonitorContextHolder.clearContext());
        // 9. 处理调用 AI 生成代码后返回的流式响应并添加到对话历史
        codeStream = streamHandlerExecutor.doExecute(codeStream, chatHistoryService, chatHistoryOriginalService, appId, loginUser, codeGenType);
        // 10. 构建特殊事件，得到SSE流
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> codeStream
                    .map(chunk -> {
                        // 将内容打包成JSON对象
                        Map<String, String> wrapper = Map.of("d", chunk);
                        String jsonStr = JSONUtil.toJsonStr(wrapper);
                        return ServerSentEvent.<String>builder()
                                .data(jsonStr)
                                .build();
                    })
                    .concatWith(Mono.just(
                            // 发送结束事件
                            ServerSentEvent.<String>builder()
                                    .data("")
                                    .event("done")
                                    .build()
                    ));
            case VUE_PROJECT -> codeStream
                    .map(chunk -> {
                        // 特殊标记：构建完成事件，转为单独的 SSE 事件类型
                        if ("__BUILD_DONE__".equals(chunk)) {
                            return ServerSentEvent.<String>builder()
                                    .event("buildDone")
                                    .data("")
                                    .build();
                        }
                        // 将内容打包成JSON对象
                        Map<String, String> wrapper = Map.of("d", chunk);
                        String jsonStr = JSONUtil.toJsonStr(wrapper);
                        return ServerSentEvent.<String>builder()
                                .data(jsonStr)
                                .build();
                    });
        };
    }

    @Override
    public AppDeployTaskVO deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证权限，仅本人部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 部署前先检查源码目录，避免把无效任务推入消息队列
        String sourceDirPath = buildSourceDirPath(app.getCodeGenType(), appId);
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(), ErrorCode.SYSTEM_ERROR, "应用代码不存在，请生成代码");
        String lockKey = "codefather:deploy:submit:" + appId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, ErrorCode.TOO_MANY_REQUEST, "当前应用正在创建部署任务，请稍后重试");
            ThrowUtils.throwIf(appDeployTaskService.hasRunningTask(appId), ErrorCode.OPERATION_ERROR, "当前应用已有部署任务在执行中");
            // 5. 检查是否已有 deployKey，实现重复部署
            String deployKey = app.getDeployKey();
            if (StrUtil.isBlank(deployKey)) {
                deployKey = RandomUtil.randomString(6);
            }
            AppDeployTask task = appDeployTaskService.createTask(appId, loginUser.getId(), deployKey);
            try {
                appDeployTaskProducer.sendDeployTask(task.getId(), appId, task.getRetryCount(), null);
            } catch (Exception e) {
                appDeployTaskService.updateTaskStatus(task.getId(), AppDeployTaskStatusEnum.FAILED, "发送部署任务消息失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送部署任务消息失败");
            }
            return appDeployTaskService.getTaskVO(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建部署任务时被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public AppDeployTaskVO getDeployTaskStatus(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        AppDeployTask task = appDeployTaskService.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "部署任务不存在");
        boolean isOwner = loginUser.getId().equals(task.getUserId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "无权限查看该部署任务");
        return appDeployTaskService.getTaskVO(task);
    }

    /**
     * 删除应用时关联删除对话历史
     * @param id 应用 ID
     * @return
     */
    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        chatHistoryService.deleteByAppId(appId);
        chatHistoryOriginalService.deleteByAppId(appId);
        return super.removeById(id);
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
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryDTO appQueryDTO) {
        if (appQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryDTO.getId();
        String appName = appQueryDTO.getAppName();
        String cover = appQueryDTO.getCover();
        String initPrompt = appQueryDTO.getInitPrompt();
        String codeGenType = appQueryDTO.getCodeGenType();
        String deployKey = appQueryDTO.getDeployKey();
        Integer priority = appQueryDTO.getPriority();
        Long userId = appQueryDTO.getUserId();
        String sortField = appQueryDTO.getSortField();
        String sortOrder = appQueryDTO.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(App::getId, id)
                .like(App::getAppName, appName)
                .like(App::getInitPrompt, initPrompt)
                .eq(App::getCodeGenType, codeGenType)
                .eq(App::getPriority, priority)
                .eq(App::getUserId, userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
        if (appQueryDTO.getDeployKey() == null) {
            return queryWrapper;
        } else {
            if ("0".equals(deployKey)) {
                // 获取未部署的
                queryWrapper.isNull(App::getDeployKey);
            } else if ("1".equals(deployKey)) {
                // 获取已部署的
                queryWrapper.isNotNull(App::getDeployKey);
            } else {
                // 获取指定 deployKey 的
                queryWrapper.eq(App::getDeployKey, deployKey);
            }
            return queryWrapper;
        }
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        // 获取应用列表的用户Id
        Set<Long> userIds = appList.stream()
                .map(App::getUserId).collect(Collectors.toSet());
        // 通过用户Id获取用户信息，存储到Map中
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 填充用户信息到应用中
        return appList.stream()
                .map(app -> {
                    AppVO appVO = getAppVO(app);
                    UserVO userVO = userVOMap.get(app.getUserId());
                    appVO.setUser(userVO);
                    return appVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Long createApp(AppAddDTO appAddDTO, User loginUser) {
        // 参数校验
        String initPrompt = appAddDTO.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddDTO, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用AI智能选择代码生成类型
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    @Override
    @Transactional
    public boolean removeByUserId(Long userId) {
        // 参数校验
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "用户不存在");
        // 查询该用户下的所有应用
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(App::getUserId, userId);
        List<App> appList = this.list(queryWrapper);
        List<Long> appIds = appList.stream().map(App::getId).collect(Collectors.toList());
        // 删除所有应用的对话历史记录
        boolean result = chatHistoryService.deleteByAppIds(appIds);
        if (!result) {
            log.error("删除应用对话历史记录失败，userId: {}", userId);
        }
        result = chatHistoryOriginalService.deleteByAppIds(appIds);
        if (!result) {
            log.error("删除应用原始对话历史记录失败，userId: {}", userId);
        }
        // 删除所有应用
        return this.removeByIds(appIds);
    }

    private String buildSourceDirPath(String codeGenType, Long appId) {
        String sourceDirName = codeGenType + "_" + appId;
        return AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
    }

}
