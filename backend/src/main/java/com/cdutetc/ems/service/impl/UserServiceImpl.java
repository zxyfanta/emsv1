package com.cdutetc.ems.service.impl;

import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.UserStatus;
import com.cdutetc.ems.repository.UserRepository;
import com.cdutetc.ems.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user) {
        log.debug("Creating user: {}", user.getUsername());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }

        // 检查邮箱是否已存在
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + user.getEmail());
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        log.debug("User created successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
    }

    @Override
    public User updateUser(Long id, User user) {
        log.debug("Updating user with ID: {}", id);

        User existingUser = findById(id);

        // 更新用户信息
        if (user.getFullName() != null) {
            existingUser.setFullName(user.getFullName());
        }
        if (user.getEmail() != null) {
            // 检查邮箱是否被其他用户使用
            Optional<User> userWithEmail = userRepository.findByEmail(user.getEmail());
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("邮箱已被其他用户使用: " + user.getEmail());
            }
            existingUser.setEmail(user.getEmail());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }

        User updatedUser = userRepository.save(existingUser);
        log.debug("User updated successfully: {}", updatedUser.getUsername());
        return updatedUser;
    }

    @Override
    public void deleteUser(Long id) {
        log.debug("Deleting user with ID: {}", id);

        User existingUser = findById(id);
        userRepository.delete(existingUser);

        log.debug("User deleted successfully: {}", existingUser.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAllWithCompany(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByCompanyId(Long companyId, Pageable pageable) {
        return userRepository.findByCompanyIdWithCompany(companyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByUsernameContaining(String username, Pageable pageable) {
        return userRepository.findByUsernameContainingWithCompany(username, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void updatePassword(Long userId, String newPassword) {
        log.debug("Updating password for user ID: {}", userId);

        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.debug("Password updated successfully for user: {}", user.getUsername());
    }

    @Override
    public void updateUserStatus(Long userId, UserStatus status) {
        log.debug("Updating status for user ID: {} to {}", userId, status);

        User user = findById(userId);
        user.setStatus(status);
        userRepository.save(user);

        log.debug("Status updated successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateUserCompanyAccess(Long userId, Long companyId) {
        try {
            User user = findById(userId);
            return user.getCompany() != null && user.getCompany().getId().equals(companyId);
        } catch (Exception e) {
            log.debug("User company access validation failed: {}", e.getMessage());
            return false;
        }
    }
}