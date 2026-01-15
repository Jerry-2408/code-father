package com.example.codefather.controller;

import cn.hutool.core.bean.BeanUtil;
import com.example.codefather.annotation.AuthCheck;
import com.example.codefather.common.BaseResponse;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.model.dto.DeleteDTO;
import com.example.codefather.common.ResultUtils;
import com.example.codefather.constant.UserConstant;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.exception.ThrowUtils;
import com.example.codefather.model.dto.user.*;
import com.example.codefather.model.vo.user.UserLoginVO;
import com.example.codefather.model.vo.user.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.codefather.model.entity.User;
import com.example.codefather.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterDTO 用户注册请求
     * @return 创建用户 ID
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        ThrowUtils.throwIf(userRegisterDTO == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterDTO.getUserAccount();
        String userPassword = userRegisterDTO.getUserPassword();
        String checkPassword = userRegisterDTO.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginDTO 用户登录请求
     * @param request 请求
     * @return 用户登录信息
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginDTO == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginDTO.getUserAccount();
        String userPassword = userLoginDTO.getUserPassword();
        return ResultUtils.success(userService.userLogin(userAccount, userPassword, request));
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    BaseResponse<UserLoginVO> getLoginUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getUserLoginVO(currentUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 是否注销成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 添加用户（仅管理员）
     *
     * @param userAddDTO 用户添加请求
     * @return 创建用户 ID
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddDTO userAddDTO) {
        ThrowUtils.throwIf(userAddDTO == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddDTO, user);
        // 默认密码
        final String DEFAULT_USER_PASSWORD = "12345678";
        user.setUserPassword(userService.getEncryptPassword(DEFAULT_USER_PASSWORD));
        // 保存数据
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(user.getId());
    }

    /**
     * 根据主键查询用户。（仅管理员）
     *
     * @param id 主键
     * @return 用户详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(user);
    }

    /**
     * 根据主键查询用户。
     *
     * @param id 主键
     * @return 用户详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户。（仅管理员）
     *
     * @param deleteDTO 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteDTO deleteDTO) {
        if (deleteDTO == null || deleteDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteDTO.getId());
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(b);
    }

    /**
     * 更新用户。（更新权限仅管理员）
     *
     * @param updateDTO 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO updateDTO,
                                            HttpServletRequest request) {
        if (updateDTO == null || updateDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(updateDTO, user);
        boolean b = userService.update(user, request);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户。（仅管理员）
     *
     * @param userQueryDTO 查询请求
     * @return 用户分页结果
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> getUserVOByPage(@RequestBody UserQueryDTO userQueryDTO) {
        ThrowUtils.throwIf(userQueryDTO == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryDTO.getPageNum();
        long pageSize = userQueryDTO.getPageSize();
        // 分页查询
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), userService.getQueryWrapper(userQueryDTO));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);

        return ResultUtils.success(userVOPage);
    }

}
