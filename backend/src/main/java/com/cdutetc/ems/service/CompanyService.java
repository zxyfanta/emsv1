package com.cdutetc.ems.service;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 企业服务接口
 */
public interface CompanyService {

    /**
     * 创建企业
     */
    Company createCompany(Company company);

    /**
     * 根据ID查找企业
     */
    Company findById(Long id);

    /**
     * 根据企业编码查找企业
     */
    Company findByCompanyCode(String companyCode);

    /**
     * 更新企业信息
     */
    Company updateCompany(Long id, Company company);

    /**
     * 删除企业
     */
    void deleteCompany(Long id);

    /**
     * 分页查询所有企业
     */
    Page<Company> findAll(Pageable pageable);

    /**
     * 根据企业名称模糊查询
     */
    Page<Company> findByCompanyNameContaining(String name, Pageable pageable);

    /**
     * 检查企业编码是否存在
     */
    boolean existsByCompanyCode(String companyCode);

    /**
     * 更新企业状态
     */
    void updateCompanyStatus(Long id, CompanyStatus status);

    /**
     * 统计活跃企业数量
     */
    long countActiveCompanies();
}