package com.ems.service;

import com.ems.exception.BusinessException;
import com.ems.exception.ValidationException;
import com.ems.entity.SystemConfig;
import com.ems.entity.User;
import com.ems.exception.ErrorCode;
import com.ems.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统配置服务层
 * 提供系统配置的增删改查功能
 *
 * @author EMS Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Cacheable(value = "systemConfig", key = "#configKey")
    public String getConfigValue(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new ValidationException("配置键不能为空");
        }

        Optional<SystemConfig> config = systemConfigRepository.findByConfigKeyAndEnabledTrue(configKey);
        return config.map(SystemConfig::getValueOrDefault).orElse(null);
    }

    /**
     * 根据配置键获取配置实体
     *
     * @param configKey 配置键
     * @return 配置实体
     */
    @Cacheable(value = "systemConfig", key = "#configKey + '_entity'")
    public Optional<SystemConfig> getConfig(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new ValidationException("配置键不能为空");
        }

        return systemConfigRepository.findByConfigKeyAndEnabledTrue(configKey);
    }

    /**
     * 获取布尔类型配置值
     *
     * @param configKey 配置键
     * @return 布尔值
     */
    public Boolean getBooleanConfig(String configKey) {
        Optional<SystemConfig> config = getConfig(configKey);
        return config.map(SystemConfig::getBooleanValue).orElse(null);
    }

    /**
     * 获取数字类型配置值
     *
     * @param configKey 配置键
     * @return 数字值
     */
    public Number getNumericConfig(String configKey) {
        Optional<SystemConfig> config = getConfig(configKey);
        return config.map(SystemConfig::getNumericValue).orElse(null);
    }

    /**
     * 获取指定分类的所有配置
     *
     * @param category 配置分类
     * @return 配置列表
     */
    public List<SystemConfig> getConfigsByCategory(SystemConfig.ConfigCategory category) {
        return systemConfigRepository.findByCategoryAndEnabledTrueOrderByConfigKey(category);
    }

    /**
     * 获取所有启用的配置
     *
     * @return 配置列表
     */
    @Cacheable(value = "systemConfig", key = "'all_enabled'")
    public List<SystemConfig> getAllEnabledConfigs() {
        return systemConfigRepository.findByEnabledTrueOrderByCategoryAscConfigKey();
    }

    /**
     * 分页查询配置
     *
     * @param pageable   分页参数
     * @param category   配置分类（可选）
     * @param configType 配置类型（可选）
     * @param configName 配置名称（可选，模糊查询）
     * @return 配置分页
     */
    public Page<SystemConfig> getConfigs(Pageable pageable,
                                        SystemConfig.ConfigCategory category,
                                        SystemConfig.ConfigType configType,
                                        String configName) {
        // 根据条件查询
        if (category != null && configType != null) {
            return systemConfigRepository.findByConfigTypeAndEnabled(configType, true, pageable);
        } else if (category != null) {
            return systemConfigRepository.findByCategoryAndEnabled(category, true, pageable);
        } else if (StringUtils.hasText(configName)) {
            return systemConfigRepository.findByConfigNameContainingIgnoreCaseAndEnabled(configName, true, pageable);
        } else {
            return systemConfigRepository.findAll(pageable);
        }
    }

    /**
     * 创建配置
     *
     * @param config 配置信息
     * @return 创建的配置
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public SystemConfig createConfig(SystemConfig config, User currentUser) {
        validateConfig(config, true);

        // 检查配置键是否已存在
        if (systemConfigRepository.existsByConfigKey(config.getConfigKey())) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE,
                    "配置键已存在: " + config.getConfigKey());
        }

        config.setUpdatedByUser(currentUser);
        SystemConfig savedConfig = systemConfigRepository.save(config);

        log.info("创建系统配置成功: key={}, user={}",
                config.getConfigKey(), currentUser.getUsername());

        return savedConfig;
    }

    /**
     * 更新配置
     *
     * @param id         配置ID
     * @param newConfig 新配置信息
     * @param currentUser 当前用户
     * @return 更新后的配置
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public SystemConfig updateConfig(Long id, SystemConfig newConfig, User currentUser) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
        if (configOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "配置不存在");
        }

        SystemConfig config = configOpt.get();

        // 系统配置不允许修改配置键和配置类型
        if (config.isSystemConfig()) {
            if (!config.getConfigKey().equals(newConfig.getConfigKey())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "系统配置不允许修改配置键");
            }
            if (!config.getConfigType().equals(newConfig.getConfigType())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "系统配置不允许修改配置类型");
            }
        }

        // 验证配置
        validateConfig(newConfig, false);

        // 更新字段
        config.setConfigName(newConfig.getConfigName());
        config.setDescription(newConfig.getDescription());
        config.setCategory(newConfig.getCategory());
        config.setConfigValue(newConfig.getConfigValue());
        config.setEnabled(newConfig.getEnabled());
        config.setOptions(newConfig.getOptions());
        config.setValidationRule(newConfig.getValidationRule());
        config.setUpdatedByUser(currentUser);

        SystemConfig savedConfig = systemConfigRepository.save(config);

        log.info("更新系统配置成功: key={}, user={}",
                config.getConfigKey(), currentUser.getUsername());

        return savedConfig;
    }

    /**
     * 更新配置值
     *
     * @param configKey 配置键
     * @param value     配置值
     * @param currentUser 当前用户
     * @return 更新后的配置
     */
    @Transactional
    @CacheEvict(value = "systemConfig", key = "#configKey")
    public SystemConfig updateConfigValue(String configKey, String value, User currentUser) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(configKey);
        if (configOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "配置不存在: " + configKey);
        }

        SystemConfig config = configOpt.get();
        config.setConfigValue(value);
        config.setUpdatedByUser(currentUser);

        SystemConfig savedConfig = systemConfigRepository.save(config);

        log.info("更新配置值成功: key={}, user={}", configKey, currentUser.getUsername());

        return savedConfig;
    }

    /**
     * 删除配置
     *
     * @param id         配置ID
     * @param currentUser 当前用户
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public void deleteConfig(Long id, User currentUser) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
        if (configOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "配置不存在");
        }

        SystemConfig config = configOpt.get();

        // 系统配置不允许删除
        if (config.isSystemConfig()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "系统配置不允许删除");
        }

        systemConfigRepository.delete(config);

        log.info("删除系统配置成功: key={}, user={}",
                config.getConfigKey(), currentUser.getUsername());
    }

    /**
     * 批量更新启用状态
     *
     * @param ids      配置ID列表
     * @param enabled  启用状态
     * @param currentUser 当前用户
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public int batchUpdateEnabled(List<Long> ids, Boolean enabled, User currentUser) {
        int updatedCount = systemConfigRepository.batchUpdateEnabled(ids, enabled);

        log.info("批量更新配置启用状态成功: count={}, enabled={}, user={}",
                updatedCount, enabled, currentUser.getUsername());

        return updatedCount;
    }

    /**
     * 获取需要重启的配置列表
     *
     * @return 配置列表
     */
    public List<SystemConfig> getRequireRestartConfigs() {
        return systemConfigRepository.findByRequireRestartTrueAndEnabled(true);
    }

    /**
     * 获取配置统计信息
     *
     * @return 统计信息
     */
    public Map<String, Long> getConfigStatistics() {
        List<Object[]> results = systemConfigRepository.countByCategory();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((SystemConfig.ConfigCategory) result[0]).getDescription(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 刷新配置缓存
     */
    @CacheEvict(value = "systemConfig", allEntries = true)
    public void refreshCache() {
        log.info("系统配置缓存已刷新");
    }

    /**
     * 验证配置信息
     *
     * @param config 配置信息
     * @param isNew  是否为新建
     */
    private void validateConfig(SystemConfig config, boolean isNew) {
        if (config == null) {
            throw new ValidationException("配置信息不能为空");
        }

        // 验证必填字段
        if (!StringUtils.hasText(config.getConfigKey())) {
            throw new ValidationException("配置键不能为空");
        }

        if (!StringUtils.hasText(config.getConfigName())) {
            throw new ValidationException("配置名称不能为空");
        }

        if (config.getCategory() == null) {
            throw new ValidationException("配置分类不能为空");
        }

        if (config.getConfigType() == null) {
            throw new ValidationException("配置类型不能为空");
        }

        // 验证配置键格式
        if (!config.getConfigKey().matches("^[a-zA-Z][a-zA-Z0-9._-]*$")) {
            throw new ValidationException("配置键格式不正确，应以字母开头，只能包含字母、数字、点、下划线和连字符");
        }

        // 验证配置值
        validateConfigValue(config);
    }

    /**
     * 验证配置值
     *
     * @param config 配置信息
     */
    private void validateConfigValue(SystemConfig config) {
        String configValue = config.getConfigValue();
        String validationRule = config.getValidationRule();

        if (StringUtils.hasText(configValue) && StringUtils.hasText(validationRule)) {
            if (!configValue.matches(validationRule)) {
                throw new ValidationException("配置值不符合验证规则: " + validationRule);
            }
        }

        // 根据配置类型进行特殊验证
        switch (config.getConfigType()) {
            case BOOLEAN:
                if (StringUtils.hasText(configValue)) {
                    if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue)) {
                        throw new ValidationException("布尔值配置只能为 true 或 false");
                    }
                }
                break;
            case EMAIL:
                if (StringUtils.hasText(configValue)) {
                    if (!configValue.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                        throw new ValidationException("邮箱格式不正确");
                    }
                }
                break;
            case URL:
                if (StringUtils.hasText(configValue)) {
                    if (!configValue.matches("^https?://[A-Za-z0-9.-]+[/#?]?.*$")) {
                        throw new ValidationException("URL格式不正确");
                    }
                }
                break;
            case NUMBER:
                if (StringUtils.hasText(configValue)) {
                    try {
                        Double.parseDouble(configValue);
                    } catch (NumberFormatException e) {
                        throw new ValidationException("数字格式不正确");
                    }
                }
                break;
        }
    }
}