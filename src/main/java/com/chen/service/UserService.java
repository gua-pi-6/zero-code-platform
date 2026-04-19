package com.chen.service;

import com.chen.model.dto.user.UserQueryRequest;
import com.chen.model.entity.User;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 提供用户注册、登录及视图转换能力。
 */
public interface UserService extends IService<User> {

    /**
     * 注册新用户。
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 对传入密码执行加密。
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String password);

    /**
     * 为新用户生成随机展示名称。
     *
     * @return 随机用户名
     */
    String getRandomUserName();

    /**
     * 将用户实体转换为登录态视图对象。
     *
     * @param user 用户实体
     * @return 登录用户视图对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 从请求中获取当前登录用户。
     *
     * @param request Servlet 请求对象
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 退出当前登录用户。
     *
     * @param request Servlet 请求对象
     * @return 是否退出成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 使用指定账号密码执行登录。
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param request Servlet 请求对象
     * @return 登录用户视图对象
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 将用户实体转换为公开视图对象。
     *
     * @param user 用户实体
     * @return 用户视图对象
     */
    UserVO getUserVO(User user);

    /**
     * 将用户实体列表转换为公开视图对象列表。
     *
     * @param userList 用户实体列表
     * @return 用户视图对象列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 构造用户列表查询条件。
     *
     * @param userQueryRequest 查询请求
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
