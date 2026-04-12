CREATE TABLE `app` (
                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                       `appName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
                       `cover` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
                       `initPrompt` text COLLATE utf8mb4_unicode_ci COMMENT '应用初始化的 prompt',
                       `codeGenType` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型（枚举）',
                       `deployKey` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部署标识',
                       `deployedTime` datetime DEFAULT NULL COMMENT '部署时间',
                       `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
                       `userId` bigint NOT NULL COMMENT '创建用户id',
                       `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
                       `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                       `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                       PRIMARY KEY (`id`),
                       UNIQUE KEY `uk_deployKey` (`deployKey`),
                       KEY `idx_appName` (`appName`),
                       KEY `idx_userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=360285596464291841 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用'

CREATE TABLE `chat_history` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                                `message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
                                `messageType` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
                                `appId` bigint NOT NULL COMMENT '应用id',
                                `userId` bigint NOT NULL COMMENT '创建用户id',
                                `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_appId` (`appId`),
                                KEY `idx_appId_createTime` (`appId`,`createTime`),
                                KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB AUTO_INCREMENT=360285862722904065 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史'

CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                        `userAccount` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
                        `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
                        `userName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
                        `userAvatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
                        `userProfile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
                        `userRole` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
                        `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
                        `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_userAccount` (`userAccount`),
                        KEY `idx_userName` (`userName`)
) ENGINE=InnoDB AUTO_INCREMENT=359602993763528705 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户'

