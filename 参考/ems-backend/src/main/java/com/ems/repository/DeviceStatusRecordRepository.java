package com.ems.repository;

import com.ems.entity.DeviceStatusRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备状态记录数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface DeviceStatusRecordRepository extends JpaRepository<DeviceStatusRecord, Long> {

    /**
     * 根据设备ID查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID分页查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId ORDER BY dsr.recordTime DESC")
    Page<DeviceStatusRecord> findByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 根据设备ID和时间范围查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 根据设备ID和时间范围分页查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime ORDER BY dsr.recordTime DESC")
    Page<DeviceStatusRecord> findByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime,
                                                        Pageable pageable);

    /**
     * 查找设备的最新状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findLatestByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 根据设备ID查找CPM记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue IS NOT NULL ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findCpmRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID和时间范围查找CPM记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue IS NOT NULL " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findCpmRecordsByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                                                 @Param("startTime") LocalDateTime startTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 根据设备ID查找电池电压记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.batteryVoltageMv IS NOT NULL ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findBatteryRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID查找电池低电量记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.batteryVoltageMv < 3700 " + // 3.7V以下认为是低电量
           "ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findLowBatteryRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID查找异常CPM记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue > 100 " + // CPM值超过100认为异常
           "ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findAbnormalCpmRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据数据来源查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.dataSource = :dataSource ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findByDataSource(@Param("dataSource") String dataSource);

    /**
     * 根据数据来源分页查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.dataSource = :dataSource ORDER BY dsr.recordTime DESC")
    Page<DeviceStatusRecord> findByDataSource(@Param("dataSource") String dataSource, Pageable pageable);

    /**
     * 根据处理状态查找状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.processingStatus = :processingStatus ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findByProcessingStatus(@Param("processingStatus") String processingStatus);

    /**
     * 查找错误状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.processingStatus = 'ERROR' ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findErrorRecords();

    /**
     * 统计设备的状态记录总数
     */
    @Query("SELECT COUNT(dsr) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId")
    long countByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 统计设备在时间范围内的状态记录总数
     */
    @Query("SELECT COUNT(dsr) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime")
    long countByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 统计不同数据来源的记录数量
     */
    @Query("SELECT dsr.dataSource, COUNT(dsr) FROM DeviceStatusRecord dsr " +
           "WHERE dsr.recordTime BETWEEN :startTime AND :endTime " +
           "GROUP BY dsr.dataSource")
    List<Object[]> countByDataSourceBetween(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计不同处理状态的记录数量
     */
    @Query("SELECT dsr.processingStatus, COUNT(dsr) FROM DeviceStatusRecord dsr " +
           "WHERE dsr.recordTime BETWEEN :startTime AND :endTime " +
           "GROUP BY dsr.processingStatus")
    List<Object[]> countByProcessingStatusBetween(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 计算设备的平均CPM值
     */
    @Query("SELECT AVG(dsr.cpmValue) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue IS NOT NULL " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageCpmByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 计算设备的最大CPM值
     */
    @Query("SELECT MAX(dsr.cpmValue) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue IS NOT NULL " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime")
    Integer getMaxCpmByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 计算设备的最小CPM值
     */
    @Query("SELECT MIN(dsr.cpmValue) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.cpmValue IS NOT NULL " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime")
    Integer getMinCpmByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 计算设备的平均电池电压
     */
    @Query("SELECT AVG(dsr.batteryVoltageMv) FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.batteryVoltageMv IS NOT NULL " +
           "AND dsr.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageBatteryVoltageByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的状态记录（全局）
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findRecentRecords(Pageable pageable);

    /**
     * 根据设备查找最近5分钟的状态记录
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.device.deviceId = :deviceId " +
           "AND dsr.recordTime >= :sinceTime " +
           "ORDER BY dsr.recordTime DESC")
    List<DeviceStatusRecord> findRecentRecordsByDeviceId(@Param("deviceId") String deviceId,
                                                        @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * 删除指定时间之前的状态记录（数据清理）
     */
    @Query("DELETE FROM DeviceStatusRecord dsr WHERE dsr.recordTime < :thresholdDate")
    void deleteByRecordTimeBefore(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * 分页查询状态记录（支持多条件过滤）
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE " +
           "(:deviceId IS NULL OR dsr.device.deviceId = :deviceId) AND " +
           "(:dataSource IS NULL OR dsr.dataSource = :dataSource) AND " +
           "(:processingStatus IS NULL OR dsr.processingStatus = :processingStatus) AND " +
           "(:startTime IS NULL OR dsr.recordTime >= :startTime) AND " +
           "(:endTime IS NULL OR dsr.recordTime <= :endTime) " +
           "ORDER BY dsr.recordTime DESC")
    Page<DeviceStatusRecord> findStatusRecordsWithFilters(@Param("deviceId") String deviceId,
                                                         @Param("dataSource") String dataSource,
                                                         @Param("processingStatus") String processingStatus,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime,
                                                         Pageable pageable);

    /**
     * 统计指定时间范围内的记录总数
     */
    @Query("SELECT COUNT(dsr) FROM DeviceStatusRecord dsr WHERE dsr.recordTime BETWEEN :startTime AND :endTime")
    Long countByRecordTimeBetween(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间范围内的记录（数据导出清理）
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DeviceStatusRecord dsr WHERE dsr.recordTime BETWEEN :startTime AND :endTime")
    Long deleteByRecordTimeBetween(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内的记录（用于导出验证）
     */
    @Query("SELECT dsr FROM DeviceStatusRecord dsr WHERE dsr.recordTime BETWEEN :startTime AND :endTime ORDER BY dsr.recordTime ASC")
    List<DeviceStatusRecord> findByRecordTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}