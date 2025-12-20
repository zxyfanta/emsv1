package com.ems.repository;

import com.ems.entity.SystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问层
 *
 * @author EMS Team
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键查找配置
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据配置键查找配置（仅启用状态）
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    Optional<SystemConfig> findByConfigKeyAndEnabledTrue(String configKey);

    /**
     * 检查配置键是否存在
     *
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 根据配置分类查找配置
     *
     * @param category 配置分类
     * @param enabled  是否启用
     * @param pageable 分页参数
     * @return 配置列表
     */
    Page<SystemConfig> findByCategoryAndEnabled(SystemConfig.ConfigCategory category, Boolean enabled, Pageable pageable);

    /**
     * 根据配置分类查找所有配置
     *
     * @param category 配置分类
     * @return 配置列表
     */
    List<SystemConfig> findByCategoryOrderByConfigKey(SystemConfig.ConfigCategory category);

    /**
     * 根据配置分类查找所有启用配置
     *
     * @param category 配置分类
     * @return 配置列表
     */
    List<SystemConfig> findByCategoryAndEnabledTrueOrderByConfigKey(SystemConfig.ConfigCategory category);

    /**
     * 查找所有启用的配置
     *
     * @return 配置列表
     */
    List<SystemConfig> findByEnabledTrueOrderByCategoryAscConfigKey();

    /**
     * 根据配置类型查找配置
     *
     * @param configType 配置类型
     * @param enabled    是否启用
     * @param pageable   分页参数
     * @return 配置列表
     */
    Page<SystemConfig> findByConfigTypeAndEnabled(SystemConfig.ConfigType configType, Boolean enabled, Pageable pageable);

    /**
     * 根据配置名称模糊查询
     *
     * @param configName 配置名称
     * @param enabled    是否启用
     * @param pageable   分页参数
     * @return 配置列表
     */
    Page<SystemConfig> findByConfigNameContainingIgnoreCaseAndEnabled(String configName, Boolean enabled, Pageable pageable);

    /**
     * 查找需要重启的配置
     *
     * @param enabled 是否启用
     * @return 配置列表
     */
    List<SystemConfig> findByRequireRestartTrueAndEnabled(Boolean enabled);

    /**
     * 查找系统配置
     *
     * @param enabled 是否启用
     * @return 配置列表
     */
    List<SystemConfig> findByIsSystemTrueAndEnabled(Boolean enabled);

    /**
     * 根据配置键前缀查找配置
     *
     * @param prefix  配置键前缀
     * @param enabled 是否启用
     * @return 配置列表
     */
    List<SystemConfig> findByConfigKeyStartingWithAndEnabledTrueOrderByConfigKey(String prefix);

    /**
     * 统计各分类的配置数量
     *
     * @return 统计结果
     */
    @Query("SELECT c.category, COUNT(c) FROM SystemConfig c WHERE c.enabled = true GROUP BY c.category")
    List<Object[]> countByCategory();

    /**
     * 查找最近修改的配置
     *
     * @param enabled  是否启用
     * @param pageable 分页参数
     * @return 配置列表
     */
    Page<SystemConfig> findByEnabledTrueOrderByUpdatedAtDesc(Boolean enabled, Pageable pageable);

    /**
     * 查找指定修改者的配置
     *
     * @param userId   用户ID
     * @param enabled  是否启用
     * @param pageable 分页参数
     * @return 配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.updatedByUser.id = :userId AND c.enabled = :enabled ORDER BY c.updatedAt DESC")
    Page<SystemConfig> findByUpdatedByAndEnabled(@Param("userId") Long userId, @Param("enabled") Boolean enabled, Pageable pageable);

    /**
     * 批量更新配置启用状态
     *
     * @param ids     配置ID列表
     * @param enabled 启用状态
     * @return 更新行数
     */
    @Query("UPDATE SystemConfig c SET c.enabled = :enabled WHERE c.id IN :ids")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
}