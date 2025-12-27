package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.DataReportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据上报日志数据访问接口
 */
@Repository
public interface DataReportLogRepository extends JpaRepository<DataReportLog, Long> {

    /**
     * 根据设备ID分页查询上报日志
     */
    Page<DataReportLog> findByDeviceId(Long deviceId, Pageable pageable);

    /**
     * 根据设备编码分页查询上报日志
     */
    Page<DataReportLog> findByDeviceCode(String deviceCode, Pageable pageable);

    /**
     * 根据设备ID和状态分页查询上报日志
     */
    Page<DataReportLog> findByDeviceIdAndStatus(Long deviceId, String status, Pageable pageable);

    /**
     * 根据设备ID和时间范围查询上报日志
     */
    @Query("SELECT l FROM DataReportLog l WHERE l.deviceId = :deviceId AND l.reportTime BETWEEN :startTime AND :endTime")
    List<DataReportLog> findByDeviceIdAndTimeRange(
            @Param("deviceId") Long deviceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备上报成功次数
     */
    @Query("SELECT COUNT(l) FROM DataReportLog l WHERE l.deviceId = :deviceId AND l.status = 'SUCCESS'")
    long countSuccessByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 统计设备上报失败次数
     */
    @Query("SELECT COUNT(l) FROM DataReportLog l WHERE l.deviceId = :deviceId AND l.status = 'FAILED'")
    long countFailedByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 统计设备总上报次数
     */
    @Query("SELECT COUNT(l) FROM DataReportLog l WHERE l.deviceId = :deviceId")
    long countByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 查询设备最近的上报日志
     */
    @Query("SELECT l FROM DataReportLog l WHERE l.deviceId = :deviceId ORDER BY l.reportTime DESC")
    List<DataReportLog> findLatestByDeviceId(@Param("deviceId") Long deviceId, Pageable pageable);

    /**
     * 查询最近的失败日志
     */
    @Query("SELECT l FROM DataReportLog l WHERE l.status = 'FAILED' ORDER BY l.reportTime DESC")
    List<DataReportLog> findLatestFailedLogs(Pageable pageable);

    /**
     * 根据上报协议查询日志
     */
    Page<DataReportLog> findByReportProtocol(String reportProtocol, Pageable pageable);
}
