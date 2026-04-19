package com.chen.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.chen.ai.AiSummaryService;
import com.chen.mapper.AppSummaryMapper;
import com.chen.model.entity.AppSummary;
import com.chen.model.entity.ChatHistory;
import com.chen.service.AppSummaryService;
import com.chen.service.ChatHistoryService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 维护讨论链路和编辑链路共用的共享摘要。
 */
@Service
@Slf4j
public class AppSummaryServiceImpl extends ServiceImpl<AppSummaryMapper, AppSummary> implements AppSummaryService {

    @Resource
    private AiSummaryService aiSummaryService;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取单个应用当前的共享摘要内容。
     *
     * @param appId 应用 id
     * @return 摘要内容
     */
    @Override
    public String getSummaryContent(Long appId) {
        // 读取摘要记录，并将空内容统一归一化为空字符串。
        AppSummary appSummary = getByAppId(appId);
        return appSummary == null ? "" : StrUtil.blankToDefault(appSummary.getSummaryContent(), "");
    }

    /**
     * 基于全部聊天历史重建共享摘要。
     *
     * @param appId 应用 id
     */
    @Override
    public void refreshSummary(Long appId) {
        // 先加载全部可用历史；若没有历史则直接跳过。
        List<ChatHistory> allHistories = listHistories(appId, null);
        if (CollUtil.isEmpty(allHistories)) {
            return;
        }

        // 从零生成新的摘要，并记录最新处理到的历史 id。
        String summaryContent = buildSummaryContent("", allHistories);
        saveOrUpdateSummary(appId, summaryContent, allHistories.getLast().getId());
    }

    /**
     * 增量刷新共享摘要。
     *
     * @param appId 应用 id
     */
    @Override
    public void refreshSummaryIncrementally(Long appId) {
        // 先读取现有摘要，确定增量处理边界。
        AppSummary existingSummary = getByAppId(appId);
        Long lastChatHistoryId = existingSummary == null ? null : existingSummary.getLastChatHistoryId();

        // 仅处理上次摘要位置之后新增的聊天记录。
        List<ChatHistory> newHistories = listHistories(appId, lastChatHistoryId);
        if (CollUtil.isEmpty(newHistories)) {
            return;
        }

        // 将旧摘要和新增记录合并，生成下一版摘要输入。
        String oldSummaryContent = existingSummary == null ? "" : StrUtil.blankToDefault(existingSummary.getSummaryContent(), "");
        String summaryContent = buildSummaryContent(oldSummaryContent, newHistories);
        saveOrUpdateSummary(appId, summaryContent, newHistories.getLast().getId());
    }

    /**
     * 删除单个应用的共享摘要。
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        // 构造仅作用于目标应用的删除条件。
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取单个应用对应的摘要记录。
     *
     * @param appId 应用 id
     * @return 摘要记录，不存在时返回 null
     */
    private AppSummary getByAppId(Long appId) {
        // 应用 id 非法时直接返回 null。
        if (appId == null || appId <= 0) {
            return null;
        }

        // 按应用维度最多读取一条摘要记录。
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.getOne(queryWrapper);
    }

    /**
     * 加载生成摘要所需的聊天历史记录。
     *
     * @param appId 应用 id
     * @param lastChatHistoryId 已处理历史的下界 id
     * @return 待摘要的聊天历史
     */
    private List<ChatHistory> listHistories(Long appId, Long lastChatHistoryId) {
        // 按 id 升序构造历史查询，确保增量处理稳定。
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("id", true);
        if (lastChatHistoryId != null && lastChatHistoryId > 0) {
            queryWrapper.gt("id", lastChatHistoryId);
        }
        return chatHistoryService.list(queryWrapper);
    }

    /**
     * 基于旧摘要和新增历史生成新的摘要内容。
     *
     * @param oldSummaryContent 旧摘要内容
     * @param histories 待摘要历史
     * @return 归一化后的摘要内容
     */
    private String buildSummaryContent(String oldSummaryContent, List<ChatHistory> histories) {
        // 将历史记录转换为摘要模型所需的结构化输入。
        List<Map<String, Object>> records = histories.stream()
                .map(history -> Map.<String, Object>of(
                        "message", StrUtil.blankToDefault(history.getMessage(), "")
                ))
                .collect(Collectors.toList());

        // 将旧摘要和新记录一起发送给摘要模型。
        String summaryInput = JSONUtil.toJsonStr(Map.of(
                "oldSummary", StrUtil.blankToDefault(oldSummaryContent, ""),
                "records", records
        ));
        String summaryContent = aiSummaryService.refreshSummary(summaryInput);
        return normalizeSummaryContent(summaryContent, oldSummaryContent);
    }

    /**
     * 持久化保存新的或更新后的摘要记录。
     *
     * @param appId 应用 id
     * @param summaryContent 摘要内容
     * @param lastChatHistoryId 最新处理到的历史 id
     */
    private void saveOrUpdateSummary(Long appId, String summaryContent, Long lastChatHistoryId) {
        // 先读取当前摘要记录，以判断走新增还是更新。
        AppSummary existingSummary = getByAppId(appId);
        if (existingSummary == null) {
            AppSummary appSummary = AppSummary.builder()
                    .appId(appId)
                    .summaryContent(summaryContent)
                    .summaryVersion(1)
                    .lastChatHistoryId(lastChatHistoryId)
                    .build();
            this.save(appSummary);
            return;
        }

        // 仅更新可变字段，并同步递增摘要版本号。
        AppSummary updateSummary = AppSummary.builder()
                .id(existingSummary.getId())
                .summaryContent(summaryContent)
                .summaryVersion(existingSummary.getSummaryVersion() == null ? 1 : existingSummary.getSummaryVersion() + 1)
                .lastChatHistoryId(lastChatHistoryId)
                .build();
        this.updateById(updateSummary);
    }

    /**
     * 在落库前归一化摘要内容。
     *
     * @param summaryContent 原始摘要内容
     * @param oldSummaryContent 旧摘要内容
     * @return 归一化后的摘要内容
     */
    private String normalizeSummaryContent(String summaryContent, String oldSummaryContent) {
        // 当模型未返回有效内容时，回退到旧摘要。
        if (StrUtil.isBlank(summaryContent)) {
            return StrUtil.blankToDefault(oldSummaryContent, "");
        }

        // 去除首尾空白，并剥离模型可能返回的 Markdown 代码块包装。
        String trimmed = summaryContent.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak > -1 && lastFence > firstLineBreak) {
                trimmed = trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
