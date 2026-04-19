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
import com.chen.model.dto.user.UserAddRequest;
import com.chen.model.dto.user.UserLoginRequest;
import com.chen.model.dto.user.UserQueryRequest;
import com.chen.model.dto.user.UserRegisterRequest;
import com.chen.model.dto.user.UserUpdateRequest;
import com.chen.model.entity.User;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.chen.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供用户注册、登录及后台管理接口。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 注册新用户。
     *
     * @param userRegisterRequest 注册请求
     * @return 新用户 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 读取字段前先校验请求体。
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        // 提取注册参数，并委托服务层完成注册。
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录。
     *
     * @param userLoginRequest 登录请求
     * @param request Servlet 请求对象
     * @return 登录用户视图对象
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 读取字段前先校验请求体。
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 提取登录凭证，并委托服务层完成登录。
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户。
     *
     * @param request Servlet 请求对象
     * @return 登录用户视图对象
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 从请求会话中获取当前登录用户。
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 当前用户退出登录。
     *
     * @param request Servlet 请求对象
     * @return 是否退出成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 在委托退出逻辑前先校验请求对象。
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员新增用户。
     *
     * @param userAddRequest 新增请求
     * @return 新用户 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 校验请求参数，并映射为实体对象。
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);

        // 管理员创建用户时，使用系统默认初始密码。
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 管理员根据 id 获取用户实体。
     *
     * @param id 用户 id
     * @return 用户实体
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        // 校验用户 id，并确认目标用户存在。
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户公开视图对象。
     *
     * @param id 用户 id
     * @return 用户视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 先复用实体查询逻辑，再转换为公开 VO。
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 管理员删除用户。
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 删除前先校验请求参数。
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新用户信息。
     *
     * @param userUpdateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 复制字段前先校验请求参数。
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 复制允许编辑的字段，并持久化更新结果。
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页查询用户公开视图列表。
     *
     * @param userQueryRequest 查询请求
     * @return 分页用户视图列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 校验请求参数，并执行分页查询。
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), userService.getQueryWrapper(userQueryRequest));

        // 将用户实体转换为脱敏后的公开视图列表。
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
