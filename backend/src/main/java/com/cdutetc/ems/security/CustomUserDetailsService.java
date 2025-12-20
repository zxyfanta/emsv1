package com.cdutetc.ems.security;

import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自定义用户详情服务
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Authenticating user '{}'", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("用户不存在: " + username);
                });

        log.debug("User found: {}, role: {}, company: {}",
                user.getUsername(),
                user.getRole(),
                user.getCompany() != null ? user.getCompany().getCompanyName() : "N/A");

        return user;
    }

    /**
     * 根据用户ID加载用户
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("用户不存在: " + userId);
                });

        return user;
    }

    /**
     * 根据邮箱加载用户
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("用户不存在: " + email);
                });

        return user;
    }
}