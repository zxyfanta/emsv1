package com.ems.service;

import com.ems.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 *
 * @author EMS Team
 */
public interface UserService {

    /**
     * 分页获取用户列表
     */
    Page<User> getUsers(String keyword, String role, String status, Long enterpriseId, Pageable pageable);

    /**
     * 搜索用户
     */
    List<User> searchUsers(String keyword, int limit);

    /**
     * 获取用户统计信息
     */
    Map<String, Object> getUserStats();

    /**
     * 批量更新用户状态
     */
    int batchUpdateUserStatus(List<Long> userIds, String status);

    /**
     * 重置用户密码
     */
    void resetUserPassword(Long userId, String newPassword);

    /**
     * 根据企业ID获取用户
     */
    List<User> getUsersByEnterpriseId(Long enterpriseId);

    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);
}