package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.DeviceActivationCode;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.ActivationCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备激活码数据访问接口
 */
@Repository
public interface DeviceActivationCodeRepository extends JpaRepository<DeviceActivationCode, Long> {

    /**
     * 根据激活码查询
     */
    Optional<DeviceActivationCode> findByCode(String code);

    /**
     * 检查激活码是否存在
     */
    boolean existsByCode(String code);

    /**
     * 根据设备查询激活码
     */
    Optional<DeviceActivationCode> findByDevice(Device device);

    /**
     * 按状态查询
     */
    List<DeviceActivationCode> findByStatus(ActivationCodeStatus status);

    /**
     * 按状态和生成时间排序查询
     */
    List<DeviceActivationCode> findByStatusOrderByGeneratedAtDesc(ActivationCodeStatus status);

    /**
     * 按生成时间范围查询
     */
    List<DeviceActivationCode> findByGeneratedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 根据使用企业查询
     */
    @Query("SELECT ac FROM DeviceActivationCode ac WHERE ac.usedByCompany.id = :companyId")
    List<DeviceActivationCode> findByUsedCompany(@Param("companyId") Long companyId);

    /**
     * 统计指定状态的激活码数量
     */
    long countByStatus(ActivationCodeStatus status);
}
