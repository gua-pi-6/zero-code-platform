package com.chen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.dto.user.UserQueryRequest;
import com.chen.model.enums.UserRoleEnum;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.chen.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.chen.model.entity.User;
import com.chen.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.chen.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author 辰
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 2.查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);

        long count = this.mapper.selectCountByQuery(queryWrapper);

        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }

        // 3.加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 4.插入数据库
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userRole(UserRoleEnum.USER.getValue())
                .userName(getRandomUserName())
                .userProfile("用户很懒, 没有任何介绍~")
                .build();

        boolean save = this.save(user);

        if (!save) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户注册失败");
        }

        return user.getId();
    }

    @Override
    /**
     * 用户登录方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HTTP请求对象，用于获取会话信息
     * @return LoginUserVO 登录用户视图对象，包含用户基本信息
     * @throws BusinessException 当参数为空或用户不存在/密码错误时抛出
     */
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.效验参数
        // 检查用户账号和密码是否为空，如果为空则抛出参数错误异常
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 2.加密
        // 对用户密码进行加密处理，确保密码安全性
        String password = this.getEncryptPassword(userPassword);
        // 3.查询用户是否存在
        // 构建查询条件，根据用户账号和加密后的密码查询用户信息
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, password);
        User user = this.mapper.selectOneByQuery(queryWrapper);

        // 如果查询结果为空，则表示用户不存在或密码错误，抛出相应异常
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 获取登录用户视图对象，用于返回给前端
        LoginUserVO loginUserVO = this.getLoginUserVO(user);

        // 4.存在则记录用户登录状态
        // 将用户信息存入会话中，维护用户登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 返回登录用户视图对象
        return loginUserVO;
    }

    /**
     * 获取当前登录用户信息的方法
     * 该方法会从session中获取用户信息，并进行有效性验证
     * 如果用户不存在或无效，会抛出未登录异常
     *
     * @param request HttpServletRequest对象，用于获取session信息
     * @return User 返回当前登录的用户信息
     * @throws BusinessException 当用户未登录或用户不存在时抛出
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1.从session中获取用户信息
        // 通过request获取session，然后根据USER_LOGIN_STATE属性名获取用户对象
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);

        // 2.如果用户不存在，则抛出异常
        // 检查currentUser对象或其ID是否为空，如果是则抛出未登录异常
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3.从数据库查询用户信息
        // 根据用户ID从数据库中获取最新的用户信息
        currentUser = this.getById(currentUser.getId());

        // 再次检查从数据库查询到的用户是否存在
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 4.返回用户信息
        // 返回经过验证的有效用户信息
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1.先判断是否登录
        Object user = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjUtil.isEmpty(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }

        // 2.删除session当中的登录信息
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;
    }

    public String getEncryptPassword(String userPassword) {
        // 加盐 加密密码
        final String salt = "chen";

        return DigestUtil.md5Hex((userPassword + salt));
    }

    public String getRandomUserName() {
        // 随机生成数字用户名
        Random random = new Random();
        int num = random.nextInt(9999);
        return Integer.toString(num);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }


    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }




}
