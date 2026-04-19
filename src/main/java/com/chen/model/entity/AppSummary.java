package com.chen.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用共享摘要实体。
 * 用于在 discuss / edit 两条链路之间共享已经确认的上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_summary")
public class AppSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("summaryContent")
    private String summaryContent;

    @Column("summaryVersion")
    private Integer summaryVersion;

    @Column("lastChatHistoryId")
    private Long lastChatHistoryId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
