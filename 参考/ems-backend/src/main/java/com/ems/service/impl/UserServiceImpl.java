package com.ems.service.impl;

import com.ems.entity.User;
import com.ems.repository.UserRepository;
import com.ems.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(String keyword, String role, String status, Long enterpriseId, Pageable pageable) {
        Page<User> users;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 有关键词时使用关键词搜索
            users = userRepository.findByKeyword(keyword.trim(), pageable);
        } else {
            // 无关键词时查找所有活跃用户
            users = userRepository.findAllActive(pageable);
        }

        // 应用筛选条件
        List<User> filteredUsers = users.getContent().stream()
                .filter(user -> {
                    boolean match = true;

                    // 角色筛选
                    if (role != null && !role.trim().isEmpty()) {
                        try {
                            User.UserRole roleEnum = User.UserRole.valueOf(role.trim());
                            match = match && user.getRole() == roleEnum;
                        } catch (IllegalArgumentException e) {
                            match = false;
                        }
                    }

                    // 企业ID筛选
                    if (enterpriseId != null) {
                        match = match && Objects.equals(user.getEnterpriseId(), enterpriseId);
                    }

                    // 启用状态筛选（如果指定了status，映射到enabled字段）
                    if (status != null && !status.trim().isEmpty()) {
                        boolean enabled = "ACTIVE".equalsIgnoreCase(status.trim());
                        match = match && Objects.equals(user.getEnabled(), enabled);
                    }

                    return match;
                })
                .collect(Collectors.toList());

        // 创建新的Page对象返回
        return new PageImpl<>(filteredUsers, pageable, users.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByKeyword(keyword.trim());

        // 清除密码信息
        users.forEach(user -> user.setPassword(null));

        // 限制返回数量
        return users.stream()
                .limit(Math.min(limit, 100))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总用户数
        stats.put("total", userRepository.countActive());

        // 活跃用户数（启用状态）
        long activeCount = userRepository.findEnabledUsers().size();
        stats.put("active", activeCount);

        // 非活跃用户数（禁用状态）
        long inactiveCount = userRepository.findDisabledUsers().size();
        stats.put("inactive", inactiveCount);

        // 锁定用户数
        long lockedCount = userRepository.countActive() - activeCount;
        stats.put("locked", lockedCount);

        // 在线用户数（最近登录的用户）
        long onlineCount = userRepository.findRecentlyLoggedIn().size();
        stats.put("online", onlineCount);

        return stats;
    }

    @Override
    @Transactional
    public int batchUpdateUserStatus(List<Long> userIds, String status) {
        int updatedCount = 0;

        boolean enabled = "ACTIVE".equalsIgnoreCase(status);

        for (Long userId : userIds) {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setEnabled(enabled);
                // 注意：User实体的updatedAt会通过@LastModifiedDate自动更新
                userRepository.save(user);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    @Override
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("用户 {} 的密码已重置", user.getUsername());
        } else {
            throw new RuntimeException("用户不存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByEnterpriseId(Long enterpriseId) {
        return userRepository.findByEnterpriseIdAndDeletedFalse(enterpriseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return userRepository.existsByUsernameAndDeletedFalse(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }
}