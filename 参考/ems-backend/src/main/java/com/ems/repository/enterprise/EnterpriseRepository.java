package com.ems.repository.enterprise;

import com.ems.entity.enterprise.Enterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 企业数据访问接口 - 简化版本
 *
 * @author EMS Team
 */
@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

    /**
     * 根据名称查找企业
     */
    Optional<Enterprise> findByName(String name);

    /**
     * 查找所有企业（按创建时间排序）
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false ORDER BY e.createdAt DESC")
    List<Enterprise> findAllActive();

    /**
     * 分页查找所有企业（按创建时间排序）
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false ORDER BY e.createdAt DESC")
    Page<Enterprise> findAllActive(Pageable pageable);

    /**
     * 统计企业数量
     */
    @Query("SELECT COUNT(e) FROM Enterprise e WHERE e.deleted = false")
    long countActive();

    /**
     * 根据名称模糊搜索企业
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false AND e.name LIKE %:keyword%")
    List<Enterprise> findByNameContaining(@Param("keyword") String keyword);

    /**
     * 根据名称模糊搜索企业（新版本兼容）
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false AND e.name LIKE CONCAT('%', :keyword, '%')")
    List<Enterprise> findByNameContainingAndDeletedFalse(@Param("keyword") String keyword);

    /**
     * 根据名称查找未删除的企业
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false AND e.name = :name")
    Optional<Enterprise> findByNameAndDeletedFalse(@Param("name") String name);

    /**
     * 根据ID查找未删除的企业
     */
    @Query("SELECT e FROM Enterprise e WHERE e.id = :id AND e.deleted = false")
    Optional<Enterprise> findByIdAndDeletedFalse(@Param("id") Long id);

    /**
     * 统计未删除的企业数量
     */
    @Query("SELECT COUNT(e) FROM Enterprise e WHERE e.deleted = false")
    long countByDeletedFalse();

    /**
     * 带条件的企业分页查询
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = false AND " +
           "(:keyword IS NULL OR e.name LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:status IS NULL OR 'APPROVED' = :status)")
    Page<Enterprise> findAllActive(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  Pageable pageable);
}