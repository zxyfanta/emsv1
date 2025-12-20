package com.ems.repository;

import com.ems.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户（未删除）
     */
    Optional<User> findByUsernameOrEmailAndDeletedFalse(String username, String email);

    /**
     * 根据用户名和企业ID查找用户（未删除）
     */
    Optional<User> findByUsernameAndEnterpriseIdAndDeletedFalse(String username, Long enterpriseId);

    /**
     * 根据ID查找用户（未删除）
     */
    Optional<User> findByIdAndDeletedFalse(Long id);

    /**
     * 检查用户名是否存在（未删除）
     */
    boolean existsByUsernameAndDeletedFalse(String username);

    /**
     * 检查邮箱是否存在（未删除）
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * 根据企业ID查找所有用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.enterpriseId = :enterpriseId AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findByEnterpriseIdAndDeletedFalse(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据企业ID分页查找用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.enterpriseId = :enterpriseId AND u.deleted = false ORDER BY u.createdAt DESC")
    Page<User> findByEnterpriseIdAndDeletedFalse(@Param("enterpriseId") Long enterpriseId, Pageable pageable);

    /**
     * 根据角色查找用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findByRoleAndDeletedFalse(@Param("role") User.UserRole role);

    /**
     * 根据角色分页查找用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deleted = false ORDER BY u.createdAt DESC")
    Page<User> findByRoleAndDeletedFalse(@Param("role") User.UserRole role, Pageable pageable);

    /**
     * 根据角色和企业ID查找用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enterpriseId = :enterpriseId AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findByRoleAndEnterpriseIdAndDeletedFalse(@Param("role") User.UserRole role, @Param("enterpriseId") Long enterpriseId);

    /**
     * 查找所有活跃用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findAllActive();

    /**
     * 分页查找所有活跃用户（未删除）
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false ORDER BY u.createdAt DESC")
    Page<User> findAllActive(Pageable pageable);

    /**
     * 统计活跃用户数量（未删除）
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false")
    long countActive();

    /**
     * 统计企业用户数量（未删除）
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enterpriseId = :enterpriseId AND u.deleted = false")
    long countByEnterpriseIdAndDeletedFalse(@Param("enterpriseId") Long enterpriseId);

    /**
     * 根据关键词搜索用户（用户名、邮箱、姓名）
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
           "(u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.fullName LIKE %:keyword%)")
    List<User> findByKeyword(@Param("keyword") String keyword);

    /**
     * 根据关键词分页搜索用户
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
           "(u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.fullName LIKE %:keyword%)")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyLoggedIn();

    /**
     * 查找从未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.lastLoginAt IS NULL ORDER BY u.createdAt DESC")
    List<User> findNeverLoggedIn();

    /**
     * 查找已启用的用户
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findEnabledUsers();

    /**
     * 查找已禁用的用户
     */
    @Query("SELECT u FROM User u WHERE u.enabled = false AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findDisabledUsers();

    /**
     * 根据企业ID和启用状态查找用户
     */
    @Query("SELECT u FROM User u WHERE u.enterpriseId = :enterpriseId AND u.enabled = :enabled AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findByEnterpriseIdAndEnabledAndDeletedFalse(@Param("enterpriseId") Long enterpriseId, @Param("enabled") Boolean enabled);
}