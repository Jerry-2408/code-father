package com.example.codefather.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.dto.user.UserQueryDTO;
import com.example.codefather.model.enums.UserRoleEnum;
import com.example.codefather.model.vo.user.UserLoginVO;
import com.example.codefather.model.vo.user.UserVO;
import com.example.codefather.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.codefather.model.entity.User;
import com.example.codefather.mapper.UserMapper;
import com.example.codefather.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.codefather.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    @Resource
    private final AppService appService;

    public UserServiceImpl(AppService appService) {
        this.appService = appService;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2.查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("Uzi");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }

        return user.getId();
    }

    @Override
    public UserLoginVO getUserLoginVO(User user) {
        if (user == null) {
            return null;
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user, userLoginVO);
        return userLoginVO;
    }

    @Override
    public UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount).eq(User::getUserPassword, encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return this.getUserLoginVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        Long userId = currentUser.getId();
        currentUser = this.mapper.selectOneById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        // 清除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
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
    public QueryWrapper getQueryWrapper(UserQueryDTO userQueryDTO) {
        if (userQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryDTO.getId();
        String userAccount = userQueryDTO.getUserAccount();
        String userName = userQueryDTO.getUserName();
        String userProfile = userQueryDTO.getUserProfile();
        String userRole = userQueryDTO.getUserRole();
        String sortField = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();
        return QueryWrapper.create()
                .eq(User::getId, id)
                .eq(User::getUserRole, userRole)
                .like(User::getUserAccount, userAccount)
                .like(User::getUserName, userName)
                .like(User::getUserProfile, userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "Habby";
        return DigestUtil.md5Hex(userPassword + SALT);
    }

    /**
     * 删除用户（连带删除其应用）
     * @param id 用户id
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        boolean result = appService.removeByUserId((Long) id);
        if (!result) {
            log.error("用户关联的应用删除失败，userId: {}", id);
            return false;
        }
        User user = User.builder()
                .id((Long) id)
                .build();
        return super.removeById(user);
    }

    @Override
    public boolean update(User user, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        // 只有管理员或者当前用户可以修改
        if (UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole()) || loginUser.getId().equals(user.getId())) {
            // 密码部分
            if (StrUtil.isNotEmpty(user.getUserPassword())) {
                if (user.getUserPassword().length() < 8) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
                }
                // 密码加密
                String encryptPassword = getEncryptPassword(user.getUserPassword());
                user.setUserPassword(encryptPassword);
            }
            // 权限部分
            if (StrUtil.isNotEmpty(user.getUserRole())) {
                if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
                }
            }
            return super.updateById(user);
        }
        return false;
    }
}
