package com.chen.controller;

import com.chen.common.BaseResponse;
import com.chen.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 标记此类为RESTful控制器，所有方法都会返回JSON格式的数据
@RequestMapping("/health") // 定义基础URL路径，所有此控制器的请求都以/health开头
public class HealthController { // 健康检查控制器类，用于提供服务状态检查接口

/**
 * 健康检查接口
 * 用于服务可用性检查，返回服务状态
 *
 * @return 返回BaseResponse<String>类型，包含"ok"表示服务正常运行
 */
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
    // 使用ResultUtils工具类封装成功响应，返回"ok"字符串
        return ResultUtils.success( "ok");
    }
}
