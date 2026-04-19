package com.chen.service;

import com.chen.model.entity.AppSummary;
import com.mybatisflex.core.service.IService;

/**
 * 管理跨聊天模式共享的应用摘要。
 */
public interface AppSummaryService extends IService<AppSummary> {

    /**
     * 获取指定应用的共享摘要内容。
     *
     * @param appId 应用 id
     * @return 摘要内容
     */
    String getSummaryContent(Long appId);

    /**
     * 基于全部聊天记录重建共享摘要。
     *
     * @param appId 应用 id
     */
    void refreshSummary(Long appId);

    /**
     * 仅基于新增聊天记录刷新共享摘要。
     *
     * @param appId 应用 id
     */
    void refreshSummaryIncrementally(Long appId);

    /**
     * 删除指定应用的共享摘要。
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);
}
