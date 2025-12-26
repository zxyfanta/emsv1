package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.entity.enums.UserRole;
import com.cdutetc.ems.entity.enums.UserStatus;
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
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据企业ID查找用户
     */
    List<User> findByCompanyId(Long companyId);

    /**
     * 根据企业ID分页查询用户
     */
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId")
    Page<User> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * 根据角色查找用户
     */
    List<User> findByRole(UserRole role);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(UserStatus status);

    /**
     * 根据企业ID和状态查找用户
     */
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.status = :status")
    List<User> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") UserStatus status);

    /**
     * 根据用户名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username%")
    Page<User> findByUsernameContaining(@Param("username") String username, Pageable pageable);

    /**
     * 根据全名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:fullName%")
    Page<User> findByFullNameContaining(@Param("fullName") String fullName, Pageable pageable);

    /**
     * 统计企业用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * 统计活跃用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();

    /**
     * 查询所有用户并预加载企业信息（用于分页列表）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company")
    Page<User> findAllWithCompany(Pageable pageable);

    /**
     * 根据企业ID查询用户并预加载企业信息（用于分页列表）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.company.id = :companyId")
    Page<User> findByCompanyIdWithCompany(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * 根据用户名模糊查询并预加载企业信息
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.username LIKE %:username%")
    Page<User> findByUsernameContainingWithCompany(@Param("username") String username, Pageable pageable);
}