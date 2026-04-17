package com.chen.service;

import com.chen.model.dto.app.AppQueryRequest;
import com.chen.model.entity.User;
import com.chen.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.chen.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author 辰
 */
public interface AppService extends IService<App> {



    /**
     * 部署应用
     *
     * @param appId 应用id
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);


    /**
     * 更新应用封面
     *
     * @param appId 应用id
     * @param appAvatarUrl 应用封面访问url
     * @return 是否更新成功
     */
    void generateAppScreenshotAsync(Long appId, String appAvatarUrl);

    /**
     * 获取应用视图对象
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用查询包装器
     *
     * @param appQueryRequest 应用查询请求
     * @return 应用查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

     /**
     * 获取应用视图对象列表
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 通过对话，生成代码
     *
     * @param message   用户消息
     * @param appId     应用id
     * @param loginUser 登录用户
     * @param chatMode  聊天模式 （chat: 聊天模式, edit: 编辑模式）
     * @return 生成的代码流
     */
    Flux<String> chatToGenCode(String message, Long appId, User loginUser, String chatMode);
}
