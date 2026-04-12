package com.chen.config;


import com.qcloud.cos.COSClient;

import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosConfig {

    /**
     * 域名
     */
    private String host;

     /**
      * 密钥Id
      */
    private String secretId;

    /**
     * 密钥Key
     */
    private String secretKey;
     /**
      * 区域
      */
    private String region;

    /**
     * 存储桶
     */
    private String bucket;


    @Bean
    COSClient cosClient() {

        // 1 初始化用户身份信息（secretId, secretKey）。
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        // 2 设置 bucket 的地域
        Region clientRegion = new Region(region);
        ClientConfig clientConfig = new ClientConfig(clientRegion);

        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
}
