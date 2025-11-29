package com.chen.controller;

import cn.hutool.core.bean.BeanUtil;
import com.chen.annotation.AuthCheck;
import com.chen.common.BaseResponse;
import com.chen.common.DeleteRequest;
import com.chen.common.ResultUtils;
import com.chen.constant.UserConstant;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.dto.user.*;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.chen.model.entity.User;
import com.chen.service.UserService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author 辰
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册请求参数，包含用户账号、密码和确认密码
     * @return 返回注册成功的用户ID
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 检查请求参数是否为空，为空则抛出参数错误异常
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取用户注册信息
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 调用用户服务进行注册，返回用户ID
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);

        // 返回注册成功的用户ID
        return ResultUtils.success(userId);
    }


    /**
     * 处理用户登录请求的接口方法
     *
     * @param userLoginRequest 包含用户登录信息的请求体，包含用户账号和密码
     * @param request          HTTP请求对象，用于获取请求相关信息
     * @return 返回一个包含登录用户信息的BaseResponse对象，其中泛型为LoginUserVO
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 如果请求体为空，则抛出参数错误异常
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 从请求体中获取用户账号和密码
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 调用userService的userLogin方法进行用户登录验证，返回登录用户信息
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        // 使用ResultUtils.success方法封装并返回成功响应，包含登录用户信息
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }





}
