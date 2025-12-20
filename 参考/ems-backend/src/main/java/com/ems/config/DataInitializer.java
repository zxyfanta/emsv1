package com.ems.config;

import com.ems.entity.device.Device;
import com.ems.entity.enterprise.Enterprise;
import com.ems.entity.User;
import com.ems.entity.DeviceType;
import com.ems.repository.device.DeviceRepository;
import com.ems.repository.enterprise.EnterpriseRepository;
import com.ems.repository.UserRepository;
import com.ems.repository.DeviceTypeRepository;
import com.ems.service.DeviceCacheService;
import com.ems.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 用于在系统启动时初始化基础数据
 *
 * @author EMS Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceCacheService deviceCacheService;
    private final DeviceTypeService deviceTypeService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("开始初始化系统基础数据...");

        // 初始化企业数据
        initializeEnterprises();

        // 初始化用户数据
        initializeUsers();

        // 初始化设备类型数据
        initializeDeviceTypes();

        // 初始化设备数据
        initializeDevices();

        // 初始化设备数据到Redis缓存
        initializeDeviceCache();

        log.info("系统基础数据初始化完成");
    }

    /**
     * 初始化企业数据
     */
    private void initializeEnterprises() {
        if (enterpriseRepository.count() == 0) {
            log.info("创建默认企业数据...");

            // 创建示例企业
            Enterprise demoEnterprise = Enterprise.builder()
                    .name("演示科技有限公司")
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            Enterprise testEnterprise = Enterprise.builder()
                    .name("测试集团")
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            enterpriseRepository.save(demoEnterprise);
            enterpriseRepository.save(testEnterprise);

            log.info("默认企业数据创建完成");
        } else {
            log.info("企业数据已存在，跳过初始化");
        }
    }

    /**
     * 初始化用户数据
     */
    private void initializeUsers() {
        log.info("检查并初始化默认用户数据...");

        // 获取企业ID
        Enterprise demoEnterprise = enterpriseRepository.findByName("演示科技有限公司").orElse(null);
        Enterprise testEnterprise = enterpriseRepository.findByName("测试集团").orElse(null);

        Long demoEnterpriseId = demoEnterprise != null ? demoEnterprise.getId() : null;
        Long testEnterpriseId = testEnterprise != null ? testEnterprise.getId() : null;

        // 确保平台管理员存在
        ensureUserExists("admin", "admin123", "admin@ems.com", "平台管理员",
                User.UserRole.PLATFORM_ADMIN, null);

        // 确保企业管理员存在
        ensureUserExists("enterprise_admin", "admin123", "admin@company.com", "企业管理员",
                User.UserRole.ENTERPRISE_ADMIN, demoEnterpriseId);

        // 确保企业用户存在
        ensureUserExists("enterprise_user", "admin123", "user@company.com", "企业用户",
                User.UserRole.ENTERPRISE_USER, demoEnterpriseId);

        // 确保测试企业管理员存在
        ensureUserExists("test_admin", "admin123", "test_admin@company.com", "测试管理员",
                User.UserRole.ENTERPRISE_ADMIN, testEnterpriseId);

        log.info("默认用户数据检查完成");
    }

    /**
     * 确保用户存在，如果不存在则创建，存在则更新密码
     */
    private void ensureUserExists(String username, String password, String email,
                                String fullName, User.UserRole role, Long enterpriseId) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            // 创建新用户
            user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .fullName(fullName)
                    .role(role)
                    .enterpriseId(enterpriseId)
                    .enabled(true)
                    .accountNonLocked(true)
                    .accountNonExpired(true)
                    .credentialsNonExpired(true)
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(user);
            log.info("创建新用户: {}", username);
        } else {
            // 更新现有用户的密码和状态
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setFullName(fullName);
            user.setEnabled(true);
            user.setAccountNonLocked(true);
            user.setAccountNonExpired(true);
            user.setCredentialsNonExpired(true);
            user.setDeleted(false);

            userRepository.save(user);
            log.info("更新用户密码和状态: {}", username);
        }
    }

    /**
     * 初始化设备类型数据
     */
    private void initializeDeviceTypes() {
        try {
            if (deviceTypeRepository.count() == 0) {
                log.info("🔧 初始化设备类型数据...");

                // 使用DeviceTypeService的初始化方法
                deviceTypeService.initializeDefaultDeviceTypes();

                // 验证初始化结果
                long typeCount = deviceTypeRepository.count();
                log.info("✅ 设备类型初始化完成: 数量={}", typeCount);

                // 预热设备类型缓存
                deviceTypeService.getEnabledDeviceTypes();

            } else {
                log.info("设备类型数据已存在，检查缓存一致性...");

                // 预热设备类型缓存
                deviceTypeService.getEnabledDeviceTypes();
                log.info("✅ 设备类型缓存预热完成");
            }

        } catch (Exception e) {
            log.error("❌ 设备类型初始化失败: {}", e.getMessage(), e);
            log.info("💡 提示：设备类型初始化失败不影响系统运行，系统将使用默认配置");
        }
    }

    /**
     * 初始化设备数据
     */
    private void initializeDevices() {
        try {
            log.info("🔧 检查并初始化测试设备数据...");

            // 获取演示企业
            Enterprise demoEnterprise = enterpriseRepository.findByName("演示科技有限公司")
                    .orElseThrow(() -> new RuntimeException("演示企业不存在"));

            // 确保我们的测试设备存在
            ensureTestDeviceExists("RAD-001", "注册辐射设备-001", Device.DeviceType.RADIATION, demoEnterprise);
            ensureTestDeviceExists("ENV-001", "注册环境设备-001", Device.DeviceType.ENVIRONMENT, demoEnterprise);

            // 确保未注册测试设备不存在（如果存在则删除，用于测试拒绝逻辑）
            deleteTestDeviceIfExists("RAD-999");
            deleteTestDeviceIfExists("ENV-999");

            log.info("✅ 测试设备数据检查完成");
            log.info("📋 已注册设备: RAD-001 (辐射), ENV-001 (环境)");
            log.info("📋 未注册设备: RAD-999 (辐射), ENV-999 (环境) - 用于测试拒绝逻辑");

        } catch (Exception e) {
            log.error("❌ 设备数据初始化失败: {}", e.getMessage(), e);
            log.info("💡 提示：设备数据初始化失败不影响系统运行");
        }
    }

    /**
     * 确保测试设备存在
     */
    private void ensureTestDeviceExists(String deviceId, String deviceName, Device.DeviceType deviceType, Enterprise enterprise) {
        Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);

        if (device == null) {
            // 创建新设备
            device = Device.builder()
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .enterprise(enterprise)
                    .status(Device.DeviceStatus.OFFLINE)
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .lastOnlineAt(LocalDateTime.now())
                    .build();

            deviceRepository.save(device);
            log.info("🆕 创建测试设备: {} ({})", deviceName, deviceId);
        } else {
            log.info("✅ 测试设备已存在: {} ({})", deviceName, deviceId);
        }
    }

    /**
     * 删除测试设备（如果存在）
     */
    private void deleteTestDeviceIfExists(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);

        if (device != null) {
            deviceRepository.delete(device);
            log.info("🗑️ 删除未注册测试设备: {}", deviceId);
        } else {
            log.debug("ℹ️ 未注册测试设备不存在（符合预期）: {}", deviceId);
        }
    }

    /**
     * 初始化设备缓存
     * 将MySQL中的活跃设备预加载到Redis缓存中
     */
    private void initializeDeviceCache() {
        try {
            log.info("🚀 开始初始化设备缓存...");

            // 检查Redis连接
            try {
                deviceCacheService.getCachedDeviceCount();
                log.info("✅ Redis连接正常");
            } catch (Exception e) {
                log.warn("⚠️ Redis连接异常，跳过设备缓存初始化: {}", e.getMessage());
                return;
            }

            // 预加载活跃设备到Redis缓存
            deviceCacheService.preloadActiveDevices();

            long cachedCount = deviceCacheService.getCachedDeviceCount();
            long dbCount = deviceRepository.count();

            log.info("✅ 设备缓存初始化完成: 缓存数量={}, 数据库总数={}", cachedCount, dbCount);

            if (cachedCount < dbCount) {
                log.info("ℹ️ 部分设备未缓存，将在首次访问时自动加载到缓存");
            }

        } catch (Exception e) {
            log.error("❌ 设备缓存初始化失败: {}", e.getMessage(), e);
            log.info("💡 提示：设备缓存初始化失败不影响系统正常运行，设备信息将在首次访问时加载到缓存");
        }
    }
}