package com.chen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.mapper.UserMapper;
import com.chen.model.dto.user.UserQueryRequest;
import com.chen.model.entity.User;
import com.chen.model.enums.UserRoleEnum;
import com.chen.model.vo.LoginUserVO;
import com.chen.model.vo.UserVO;
import com.chen.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.chen.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 提供用户注册、登录和视图转换逻辑。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 注册新用户。
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 先校验注册参数。
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

        // 校验账号是否已经被注册。
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }

        // 对密码加密，并组装新的用户记录。
        String encryptPassword = getEncryptPassword(userPassword);
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userRole(UserRoleEnum.USER.getValue())
                .userName(getRandomUserName())
                .userProfile("用户很懒, 没有任何介绍~")
                .build();

        // 持久化用户记录，并返回生成后的用户 id。
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户注册失败");
        }
        return user.getId();
    }

    /**
     * 使用指定账号密码执行登录。
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param request Servlet 请求对象
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 先校验登录参数。
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 查询数据库前，先对输入密码进行加密。
        String password = this.getEncryptPassword(userPassword);

        // 根据账号和加密后的密码查询用户。
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, password);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 转换为登录视图对象，并将登录态写入 Session。
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return loginUserVO;
    }

    /**
     * 从 Session 中获取当前登录用户。
     *
     * @param request Servlet 请求对象
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先从 Session 中读取当前用户快照。
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 再从数据库重新加载用户，避免 Session 数据过期。
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 退出当前登录用户。
     *
     * @param request Servlet 请求对象
     * @return 是否退出成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 会话中不存在登录态时，拒绝执行退出。
        Object user = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjUtil.isEmpty(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }

        // 从 Session 中移除登录态。
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 使用项目本地盐值策略对密码加密。
     *
     * @param userPassword 原始密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 使用当前项目约定的固定盐值。
        final String salt = "chen";
        return DigestUtil.md5Hex(userPassword + salt);
    }

    /**
     * 生成随机默认用户名。
     *
     * @return 随机用户名
     */
    @Override
    public String getRandomUserName() {
        // 为默认用户生成简单的数字型展示名称。
        Random random = new Random();
        int num = random.nextInt(9999);
        return Integer.toString(num);
    }

    /**
     * 将用户实体转换为登录用户视图对象。
     *
     * @param user 用户实体
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        // 登录态 VO 必须来自真实用户，空对象直接拒绝。
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    /**
     * 将用户实体转换为公开用户视图对象。
     *
     * @param user 用户实体
     * @return 用户视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        // 没有源实体时直接返回 null。
        if (user == null) {
            return null;
        }

        // 仅复制允许对外暴露的字段到目标 VO。
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 将用户实体列表转换为公开用户视图列表。
     *
     * @param userList 用户实体列表
     * @return 用户视图列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        // 无数据时直接返回空列表。
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        // 将每个用户实体映射为对应的公开 VO。
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 构造用户列表查询条件。
     *
     * @param userQueryRequest 查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 尽早拦截空查询请求。
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 提取请求中支持的筛选与排序字段。
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        // 基于提取出的字段组装查询条件。
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
}
