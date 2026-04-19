package com.chen.service;

import com.chen.model.dto.app.AppQueryRequest;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 提供应用查询、部署等核心能力。
 */
public interface AppService extends IService<App> {

    /**
     * 为当前用户部署已生成的应用。
     *
     * @param appId 应用 id
     * @param loginUser 当前登录用户
     * @return 部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成并上传应用截图。
     *
     * @param appId 应用 id
     * @param appAvatarUrl 应用预览地址
     */
    void generateAppScreenshotAsync(Long appId, String appAvatarUrl);

    /**
     * 将应用实体转换为视图对象。
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 构造应用列表查询条件。
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 将应用实体列表转换为视图对象列表。
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);
}
