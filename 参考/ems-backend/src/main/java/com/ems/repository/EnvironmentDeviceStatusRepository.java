package com.ems.repository;

import com.ems.entity.device.Device;
import com.ems.entity.EnvironmentDeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测站状态记录数据访问接口
 * 提供基于实际设备数据格式的查询方法
 *
 * @author EMS Team
 */
@Repository
public interface EnvironmentDeviceStatusRepository extends JpaRepository<EnvironmentDeviceStatus, Long> {

    /**
     * 根据设备ID查找所有记录（按时间倒序）
     */
    List<EnvironmentDeviceStatus> findByDeviceOrderByRecordTimeDesc(Device device);

    /**
     * 根据设备ID和时间范围查找记录
     */
    List<EnvironmentDeviceStatus> findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
            Device device, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据设备ID和时间范围查找记录（分页）
     */
    Page<EnvironmentDeviceStatus> findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
            Device device, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据设备ID查找CPM记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.device = :device AND e.cpmValue IS NOT NULL ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findCpmRecordsByDevice(@Param("device") Device device);

    /**
     * 根据设备ID和时间范围查找CPM记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.device = :device AND e.cpmValue IS NOT NULL " +
           "AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findCpmRecordsByDeviceAndTimeRange(@Param("device") Device device,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查找高CPM值记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.cpmValue > 50 " +
           "AND e.recordTime >= :since ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findHighCpmRecords(@Param("since") LocalDateTime since);

    /**
     * 查找温度异常记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.temperature < -10 OR e.temperature > 50 " +
           "AND e.recordTime >= :since ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findAbnormalTemperatureRecords(@Param("since") LocalDateTime since);

    /**
     * 查找湿度异常记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.wetness < 20 OR e.wetness > 90 " +
           "AND e.recordTime >= :since ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findAbnormalHumidityRecords(@Param("since") LocalDateTime since);

    /**
     * 查找低电量记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.batteryVoltage < 12.0 " +
           "AND e.recordTime >= :since ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findLowBatteryRecords(@Param("since") LocalDateTime since);

    /**
     * 查找最近的记录（指定设备ID）
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.device = :device ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findLatestByDevice(@Param("device") Device device, Pageable pageable);

    /**
     * 查找最新的综合环境指数记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.totalEnvironmentIndex IS NOT NULL " +
           "ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findLatestEnvironmentIndexRecords(Pageable pageable);

    /**
     * 统计设备的平均温度
     */
    @Query("SELECT AVG(e.temperature) FROM EnvironmentDeviceStatus e WHERE e.device = :device " +
           "AND e.temperature IS NOT NULL AND e.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageTemperature(@Param("device") Device device,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的平均湿度
     */
    @Query("SELECT AVG(e.wetness) FROM EnvironmentDeviceStatus e WHERE e.device = :device " +
           "AND e.wetness IS NOT NULL AND e.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageHumidity(@Param("device") Device device,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的平均风速
     */
    @Query("SELECT AVG(e.windSpeed) FROM EnvironmentDeviceStatus e WHERE e.device = :device " +
           "AND e.windSpeed IS NOT NULL AND e.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageWindSpeed(@Param("device") Device device,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的平均综合环境指数
     */
    @Query("SELECT AVG(e.totalEnvironmentIndex) FROM EnvironmentDeviceStatus e WHERE e.device = :device " +
           "AND e.totalEnvironmentIndex IS NOT NULL AND e.recordTime BETWEEN :startTime AND :endTime")
    BigDecimal getAverageEnvironmentIndex(@Param("device") Device device,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查找环境舒适度优秀的记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE " +
           "(e.temperature BETWEEN 20 AND 30) AND (e.wetness BETWEEN 45 AND 65) " +
           "AND e.totalEnvironmentIndex >= 100 " +
           "AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findComfortableRecords(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查找风速异常记录
     */
    @Query("SELECT e FROM EnvironmentDeviceStatus e WHERE e.windSpeed < 0 OR e.windSpeed > 30 " +
           "AND e.recordTime >= :since ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceStatus> findAbnormalWindSpeedRecords(@Param("since") LocalDateTime since);

    /**
     * 查找设备健康统计
     */
    @Query("SELECT " +
           "COUNT(e) as totalRecords, " +
           "COUNT(CASE WHEN e.cpmValue > 50 THEN 1 END) as highCpmCount, " +
           "COUNT(CASE WHEN e.temperature < -10 OR e.temperature > 50 THEN 1 END) as abnormalTempCount, " +
           "COUNT(CASE WHEN e.wetness < 20 OR e.wetness > 90 THEN 1 END) as abnormalHumidityCount, " +
           "COUNT(CASE WHEN e.batteryVoltage < 12.0 THEN 1 END) as lowBatteryCount " +
           "FROM EnvironmentDeviceStatus e WHERE e.device = :device " +
           "AND e.recordTime >= :since")
    Object[] getDeviceHealthStatistics(@Param("device") Device device,
                                     @Param("since") LocalDateTime since);

    /**
     * 清理过期数据
     */
    @Query(value = "DELETE FROM environment_device_status WHERE record_time < :threshold", nativeQuery = true)
    int deleteOldRecords(@Param("threshold") LocalDateTime threshold);
}