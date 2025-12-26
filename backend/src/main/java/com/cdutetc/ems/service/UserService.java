package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 创建用户
     */
    User createUser(User user);

    /**
     * 根据ID查找用户
     */
    User findById(Long id);

    /**
     * 根据ID查找用户并加载企业信息
     */
    User findByIdWithCompany(Long id);

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 更新用户信息
     */
    User updateUser(Long id, User user);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 分页查询所有用户
     */
    Page<User> findAll(Pageable pageable);

    /**
     * 根据企业ID分页查询用户
     */
    Page<User> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * 根据用户名模糊查询
     */
    Page<User> findByUsernameContaining(String username, Pageable pageable);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 更新用户密码
     */
    void updatePassword(Long userId, String newPassword);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long userId, UserStatus status);

    /**
     * 验证用户是否可以访问企业数据
     */
    boolean validateUserCompanyAccess(Long userId, Long companyId);
}