package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 企业数据访问接口
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * 根据企业编码查找企业
     */
    Optional<Company> findByCompanyCode(String companyCode);

    /**
     * 检查企业编码是否存在
     */
    boolean existsByCompanyCode(String companyCode);

    /**
     * 根据企业名称查找企业
     */
    Optional<Company> findByCompanyName(String companyName);

    /**
     * 根据状态查找企业
     */
    List<Company> findByStatus(CompanyStatus status);

    /**
     * 分页查询企业
     */
    Page<Company> findAll(Pageable pageable);

    /**
     * 根据企业名称模糊查询
     */
    @Query("SELECT c FROM Company c WHERE c.companyName LIKE %:name%")
    Page<Company> findByCompanyNameContaining(@Param("name") String name, Pageable pageable);

    /**
     * 统计活跃企业数量
     */
    @Query("SELECT COUNT(c) FROM Company c WHERE c.status = 'ACTIVE'")
    long countActiveCompanies();
}