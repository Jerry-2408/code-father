package com.example.codefather.service;

import com.example.codefather.model.dto.app.AppQueryDTO;
import com.example.codefather.model.vo.app.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.example.codefather.model.entity.App;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
public interface AppService extends IService<App> {

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

}
