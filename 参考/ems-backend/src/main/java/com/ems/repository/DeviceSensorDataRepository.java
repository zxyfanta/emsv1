package com.ems.repository;

import com.ems.entity.DeviceSensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备传感器数据Repository
 */
@Repository
public interface DeviceSensorDataRepository extends JpaRepository<DeviceSensorData, Long> {

    /**
     * 根据设备ID、指标名称和时间范围查询数据
     */
    List<DeviceSensorData> findByDeviceIdAndMetricNameAndRecordedAtBetween(
        String deviceId, String metricName, LocalDateTime startTime, LocalDateTime endTime
    );

    /**
     * 根据设备ID和时间范围查询所有指标数据
     */
    List<DeviceSensorData> findByDeviceIdAndRecordedAtBetween(
        String deviceId, LocalDateTime startTime, LocalDateTime endTime
    );

    /**
     * 根据企业ID和时间范围查询数据
     * 注意：由于sensor_data表没有enterprise_id字段，此方法暂时不可用
     * TODO: 需要通过device表关联来查询企业数据
     */
    // List<DeviceSensorData> findByEnterpriseIdAndRecordedAtBetween(
    //     Long enterpriseId, LocalDateTime startTime, LocalDateTime endTime
    // );

    /**
     * 根据企业ID、指标名称和时间范围查询数据
     * 注意：由于sensor_data表没有enterprise_id字段，此方法暂时不可用
     * TODO: 需要通过device表关联来查询企业数据
     */
    // List<DeviceSensorData> findByEnterpriseIdAndMetricNameAndRecordedAtBetween(
    //     Long enterpriseId, String metricName, LocalDateTime startTime, LocalDateTime endTime
    // );

    /**
     * 根据时间范围查询数据
     */
    List<DeviceSensorData> findByRecordedAtBetween(
        LocalDateTime startTime, LocalDateTime endTime
    );

    /**
     * 查询指定设备最新的传感器数据
     */
    @Query("SELECT dsd FROM DeviceSensorData dsd WHERE dsd.deviceId = :deviceId " +
           "AND dsd.metricName = :metricName ORDER BY dsd.recordedAt DESC")
    List<DeviceSensorData> findLatestByDeviceAndMetric(
        @Param("deviceId") String deviceId,
        @Param("metricName") String metricName
    );

    /**
     * 查询指定设备最新的传感器数据（单条）
     */
    @Query("SELECT dsd FROM DeviceSensorData dsd WHERE dsd.deviceId = :deviceId " +
           "AND dsd.metricName = :metricName ORDER BY dsd.recordedAt DESC LIMIT 1")
    Optional<DeviceSensorData> findLatestByDeviceAndMetricLimitOne(
        @Param("deviceId") String deviceId,
        @Param("metricName") String metricName
    );

    /**
     * 查询指定时间范围内所有活跃设备
     */
    @Query("SELECT DISTINCT dsd.deviceId FROM DeviceSensorData dsd " +
           "WHERE dsd.recordedAt >= :startTime AND dsd.recordedAt < :endTime")
    List<String> findActiveDevicesByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询指定时间范围内企业的所有活跃设备
     * 注意：由于sensor_data表没有enterprise_id字段，此方法暂时不可用
     * TODO: 需要通过device表关联来查询企业数据
     */
    // @Query("SELECT DISTINCT dsd.deviceId FROM DeviceSensorData dsd " +
    //        "WHERE dsd.enterpriseId = :enterpriseId " +
    //        "AND dsd.recordedAt >= :startTime AND dsd.recordedAt < :endTime")
    // List<Long> findActiveDevicesByEnterpriseAndTimeRange(
    //     @Param("enterpriseId") Long enterpriseId,
    //     @Param("startTime") LocalDateTime startTime,
    //     @Param("endTime") LocalDateTime endTime
    // );

    /**
     * 统计指定时间范围内的数据量
     */
    @Query("SELECT COUNT(dsd) FROM DeviceSensorData dsd " +
           "WHERE dsd.recordedAt >= :startTime AND dsd.recordedAt < :endTime")
    Long countByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计指定设备在时间范围内的数据量
     */
    @Query("SELECT COUNT(dsd) FROM DeviceSensorData dsd " +
           "WHERE dsd.deviceId = :deviceId AND dsd.metricName = :metricName " +
           "AND dsd.recordedAt >= :startTime AND dsd.recordedAt < :endTime")
    Long countByDeviceAndMetricAndTimeRange(
        @Param("deviceId") String deviceId,
        @Param("metricName") String metricName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询需要清理的过期数据
     */
    List<DeviceSensorData> findByRecordedAtBefore(LocalDateTime expireTime);

    /**
     * 删除过期数据
     */
    @Query("DELETE FROM DeviceSensorData dsd WHERE dsd.recordedAt < :expireTime")
    int deleteExpiredData(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 聚合统计查询：按设备统计指定时间范围的数据
     */
    @Query("SELECT dsd.deviceId, COUNT(dsd) as recordCount, AVG(dsd.value) as avgValue, " +
           "MAX(dsd.value) as maxValue, MIN(dsd.value) as minValue " +
           "FROM DeviceSensorData dsd WHERE dsd.recordedAt >= :startTime " +
           "AND dsd.recordedAt < :endTime AND dsd.metricName = :metricName " +
           "GROUP BY dsd.deviceId ORDER BY recordCount DESC")
    List<Object[]> getDeviceStatisticsByTimeRange(
        @Param("metricName") String metricName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询异常数据
     */
    @Query("SELECT dsd FROM DeviceSensorData dsd WHERE dsd.recordedAt >= :startTime " +
           "AND dsd.recordedAt < :endTime AND (" +
           "(dsd.metricName = 'CPM' AND (dsd.value < 0 OR dsd.value > 100000)) OR " +
           "(dsd.metricName = 'Batvolt' AND (dsd.value < 2000 OR dsd.value > 5000))" +
           ")")
    List<DeviceSensorData> findAnomalousDataByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按指标分组统计数据量
     */
    @Query("SELECT dsd.metricName, COUNT(dsd) as recordCount, AVG(dsd.value) as avgValue " +
           "FROM DeviceSensorData dsd WHERE dsd.recordedAt >= :startTime " +
           "AND dsd.recordedAt < :endTime " +
           "GROUP BY dsd.metricName ORDER BY recordCount DESC")
    List<Object[]> getMetricStatisticsByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}