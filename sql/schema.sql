CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text COLLATE utf8mb4_unicode_ci COMMENT '应用初始化 prompt',
  `codeGenType` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型',
  `deployKey` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部署标识',
  `deployedTime` datetime DEFAULT NULL COMMENT '部署时间',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
  `userId` bigint NOT NULL COMMENT '创建用户 id',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deployKey` (`deployKey`),
  KEY `idx_appName` (`appName`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用';

CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `messageType` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型 user/ai',
  `chatMode` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'edit' COMMENT '对话模式 edit/chat',
  `sessionId` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '一轮会话 id',
  `appId` bigint NOT NULL COMMENT '应用 id',
  `userId` bigint NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appId` (`appId`),
  KEY `idx_appId_chatMode_createTime` (`appId`, `chatMode`, `createTime`),
  KEY `idx_sessionId` (`sessionId`),
  KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史';

CREATE TABLE `app_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appId` bigint NOT NULL COMMENT '应用 id',
  `summaryContent` text COLLATE utf8mb4_unicode_ci COMMENT '共享摘要内容',
  `summaryVersion` int NOT NULL DEFAULT '1' COMMENT '摘要版本号',
  `lastChatHistoryId` bigint DEFAULT NULL COMMENT '最后处理的对话历史 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appId` (`appId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用共享摘要';

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色 user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';
