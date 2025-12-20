package com.ems.security;

import com.ems.entity.User;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自定义用户详情服务
 *
 * @author EMS Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", usernameOrEmail);

        // 用户名或邮箱查找用户
        User user = userRepository.findByUsernameOrEmailAndDeletedFalse(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        log.debug("User found: {}, role: {}", user.getUsername(), user.getRole());
        return user;
    }

    /**
     * 根据用户ID加载用户
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        log.debug("User found: {}, role: {}", user.getUsername(), user.getRole());
        return user;
    }

    /**
     * 根据用户名和企业ID加载用户
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsernameAndEnterpriseId(String username, Long enterpriseId)
            throws UsernameNotFoundException {
        log.debug("Loading user by username: {} and enterpriseId: {}", username, enterpriseId);

        User user = userRepository.findByUsernameAndEnterpriseIdAndDeletedFalse(username, enterpriseId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username + " and enterpriseId: " + enterpriseId));

        log.debug("User found: {}, role: {}", user.getUsername(), user.getRole());
        return user;
    }
}