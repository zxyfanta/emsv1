package com.cdutetc.ems.service.impl;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.service.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 企业服务实现类
 */
@Slf4j
@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public Company createCompany(Company company) {
        log.debug("Creating company: {}", company.getCompanyCode());

        // 检查企业编码是否已存在
        if (companyRepository.existsByCompanyCode(company.getCompanyCode())) {
            throw new IllegalArgumentException("企业编码已存在: " + company.getCompanyCode());
        }

        // 设置默认状态
        if (company.getStatus() == null) {
            company.setStatus(CompanyStatus.ACTIVE);
        }

        Company savedCompany = companyRepository.save(company);
        log.debug("Company created successfully: {}", savedCompany.getCompanyCode());
        return savedCompany;
    }

    @Override
    @Transactional(readOnly = true)
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("企业不存在: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Company findByCompanyCode(String companyCode) {
        return companyRepository.findByCompanyCode(companyCode)
                .orElseThrow(() -> new IllegalArgumentException("企业不存在: " + companyCode));
    }

    @Override
    public Company updateCompany(Long id, Company company) {
        log.debug("Updating company with ID: {}", id);

        Company existingCompany = findById(id);

        // 更新企业信息
        if (company.getCompanyCode() != null) {
            existingCompany.setCompanyCode(company.getCompanyCode());
        }
        if (company.getCompanyName() != null) {
            existingCompany.setCompanyName(company.getCompanyName());
        }
        if (company.getContactEmail() != null) {
            existingCompany.setContactEmail(company.getContactEmail());
        }
        if (company.getContactPhone() != null) {
            existingCompany.setContactPhone(company.getContactPhone());
        }
        if (company.getAddress() != null) {
            existingCompany.setAddress(company.getAddress());
        }
        if (company.getStatus() != null) {
            existingCompany.setStatus(company.getStatus());
        }

        Company updatedCompany = companyRepository.save(existingCompany);
        log.debug("Company updated successfully: {}", updatedCompany.getCompanyCode());
        return updatedCompany;
    }

    @Override
    public void deleteCompany(Long id) {
        log.debug("Deleting company with ID: {}", id);

        Company existingCompany = findById(id);
        companyRepository.delete(existingCompany);

        log.debug("Company deleted successfully: {}", existingCompany.getCompanyCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> findAll(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Company> findByCompanyNameContaining(String name, Pageable pageable) {
        return companyRepository.findByCompanyNameContaining(name, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyCode(String companyCode) {
        return companyRepository.existsByCompanyCode(companyCode);
    }

    @Override
    public void updateCompanyStatus(Long id, CompanyStatus status) {
        log.debug("Updating status for company ID: {} to {}", id, status);

        Company company = findById(id);
        company.setStatus(status);
        companyRepository.save(company);

        log.debug("Status updated successfully for company: {}", company.getCompanyCode());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveCompanies() {
        return companyRepository.countActiveCompanies();
    }
}