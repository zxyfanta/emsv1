package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.VideoDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 视频设备Repository
 */
@Repository
public interface VideoDeviceRepository extends JpaRepository<VideoDevice, Long> {

    /**
     * 按编码查找
     */
    @EntityGraph(attributePaths = {"linkedDevice", "company"})
    Optional<VideoDevice> findByDeviceCode(String deviceCode);

    /**
     * 按企业查找（分页）
     */
    @EntityGraph(attributePaths = {"linkedDevice", "company"})
    Page<VideoDevice> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * 按绑定的监测设备查找
     */
    @EntityGraph(attributePaths = {"linkedDevice", "company"})
    Optional<VideoDevice> findByLinkedDeviceId(Long deviceId);

    /**
     * 查找未绑定的视频设备
     */
    @Query("SELECT v FROM VideoDevice v WHERE v.company.id = :companyId AND v.linkedDevice IS NULL")
    @EntityGraph(attributePaths = {"company"})
    List<VideoDevice> findUnboundByCompanyId(@Param("companyId") Long companyId);

    /**
     * 检查编码是否存在
     */
    boolean existsByDeviceCode(String deviceCode);

    /**
     * 按企业查找所有视频设备（不分页）
     */
    @EntityGraph(attributePaths = {"linkedDevice", "company"})
    @Query("SELECT v FROM VideoDevice v WHERE v.company.id = :companyId ORDER BY v.createdAt DESC")
    List<VideoDevice> findAllByCompanyId(@Param("companyId") Long companyId);
}
