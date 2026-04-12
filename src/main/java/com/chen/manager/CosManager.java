package com.chen.manager;

import com.chen.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;


@Slf4j
@Component
public class CosManager {


    @Resource
    private CosConfig cosConfig;

    @Resource
    private COSClient cosClient;


    public String uploadFileToCos(File filePath, String key) {

        // 获取上传到COS的请求对象
        PutObjectRequest putObjectRequest = putObjectRequest(filePath, key);

        // 发送上传请求
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

        // 判断是否上传成功
        if (putObjectResult == null) {
            log.error("上传文件到COS失败, filePath: {}", filePath);
            return null;
        }

        // 构建上传的COS文件访问路径
        String cosPath = cosConfig.getHost() + "/" + key;
        log.info("上传文件到COS成功, cosPath: {}", cosPath);
        return cosPath;
    }

    /**
     * 获取PutObjectRequest
     *
     * @param filePath 文件路径
     * @param key      COS上的对象键
     * @return PutObjectRequest
     */
    private PutObjectRequest putObjectRequest(File filePath, String key) {
        // 指定文件将要存放的存储桶
        String bucketName = cosConfig.getBucket();
        // 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下
        return new PutObjectRequest(bucketName, key, filePath);
    }

}

