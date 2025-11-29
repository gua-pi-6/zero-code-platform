package com.chen.service;

import com.chen.model.dto.user.UserQueryRequest;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.chen.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author 辰
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码字符串
     *
     * @param password 原始密码字符串
     * @return 加密后的密码字符串
     */
    String getEncryptPassword(String password);

    /**
     * 获取随机用户名的方法
     * 该方法用于生成一个随机的用户名字符串
     *
     * @return 返回一个随机生成的用户名字符串
     */
    String getRandomUserName();

    /**
     * 获取登录用户视图对象
     * 该方法用于将User实体对象转换为LoginUserVO视图对象，通常用于前端展示用户信息
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return LoginUserVO 登录用户视图对象，包含前端需要的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * <p>
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 根据User对象获取UserVO对象
     * UserVO是User的视图对象，通常用于前端展示，包含需要展示的用户信息
     *
     * @param user 用户实体对象，包含用户的所有信息
     * @return UserVO 用户视图对象，包含需要展示的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 根据用户列表获取用户视图对象列表
     * 该方法用于将User实体列表转换为UserVO视图对象列表，通常用于数据展示层
     *
     * @param userList 用户实体列表，包含完整的用户信息
     * @return 返回用户视图对象列表，可能只包含部分展示所需的信息
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据用户查询请求参数构造查询包装器
     *
     * @param userQueryRequest 用户查询请求参数对象，包含查询条件
     * @return 返回一个QueryWrapper对象，用于构建数据库查询条件
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
