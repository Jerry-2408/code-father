package com.example.codefather.service;

import com.example.codefather.model.dto.app.AppAddDTO;
import com.example.codefather.model.dto.app.AppQueryDTO;
import com.example.codefather.model.entity.User;
import com.example.codefather.model.vo.app.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.example.codefather.model.entity.App;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
public interface AppService extends IService<App> {

    /**
     * 调用大模型并生成代码文件
     *
     * @param appId 应用ID
     * @param message 消息
     * @param loginUser 登录用户
     * @return 流式结果
     */
    Flux<ServerSentEvent<String>> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署路径
     */
    public String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图
     * @param appId 应用ID
     * @param appUrl 应用URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取应用视图对象
     * @param app 应用
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询包装器
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryDTO appQueryDTO);

    /**
     * 获取应用视图对象列表
     * @param appList 应用列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 创建应用
     * @param appAddDTO 应用添加参数
     * @param loginUser 登录用户
     * @return 应用ID
     */
    Long createApp(AppAddDTO appAddDTO, User loginUser);

}
