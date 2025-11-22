package com.chen.aop;


import com.chen.annotation.AuthCheck;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.entity.User;
import com.chen.model.enums.UserRoleEnum;
import com.chen.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.chen.constant.UserConstant.USER_LOGIN_STATE;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String s = authCheck.mustRole();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User user = userService.getLoginUser(httpServletRequest);

        // 获取方法权限信息
        UserRoleEnum methodRole = UserRoleEnum.getEnumByValue(s);

        // 不需要权限
        if (methodRole == null ){
            return joinPoint.proceed();
        }

        // 获取用户权限信息
        UserRoleEnum userRole = UserRoleEnum.getEnumByValue(user.getUserRole());

        // 用户没有权限信息,直接拒绝
        if (userRole == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 要求权限必须为管理员权限,但是用户没有这个权限
        if (UserRoleEnum.ADMIN.equals(methodRole) && !UserRoleEnum.ADMIN.equals(userRole) ){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();
    }

}
