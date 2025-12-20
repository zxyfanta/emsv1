package com.ems.repository;

import com.ems.entity.RadiationDeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 辐射设备状态记录数据访问接口
 * 提供基于实际设备数据格式的查询方法
 *
 * @author EMS Team
 */
@Repository
public interface RadiationDeviceStatusRepository extends JpaRepository<RadiationDeviceStatus, Long> {

    /**
     * 根据设备查找所有记录（按时间倒序）
     */
    List<RadiationDeviceStatus> findByDeviceOrderByRecordTimeDesc(com.ems.entity.device.Device device);

    /**
     * 根据设备和时间范围查找记录
     */
    List<RadiationDeviceStatus> findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
            com.ems.entity.device.Device device, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据设备和时间范围查找记录（分页）
     */
    Page<RadiationDeviceStatus> findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
            com.ems.entity.device.Device device, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据设备查找CPM记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.device = :device AND r.cpmValue IS NOT NULL ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findCpmRecordsByDevice(@Param("device") com.ems.entity.device.Device device);

    /**
     * 根据设备和时间范围查找CPM记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.device = :device AND r.cpmValue IS NOT NULL " +
           "AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findCpmRecordsByDeviceAndTimeRange(@Param("device") com.ems.entity.device.Device device,
                                                                   @Param("startTime") LocalDateTime startTime,
                                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查找高CPM值记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.cpmValue > 100 " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findHighCpmRecords(@Param("since") LocalDateTime since);

    /**
     * 查找低电量记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.batteryVoltageMv < 3500 " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findLowBatteryRecords(@Param("since") LocalDateTime since);

    /**
     * 查找温度异常记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.deviceTemperature < -20 OR r.deviceTemperature > 60 " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findAbnormalTemperatureRecords(@Param("since") LocalDateTime since);

    /**
     * 查找最近的记录（指定设备）
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.device = :device ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findLatestByDevice(@Param("device") com.ems.entity.device.Device device, Pageable pageable);

    /**
     * 查找有GPS定位的记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.device = :device AND " +
           "(r.bdsUseful = true OR r.lbsUseful = true) ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findLocationRecordsByDevice(@Param("device") com.ems.entity.device.Device device);

    /**
     * 查找BDS定位记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.bdsUseful = true " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findBdsLocationRecords(@Param("since") LocalDateTime since);

    /**
     * 查找LBS定位记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.lbsUseful = true " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findLbsLocationRecords(@Param("since") LocalDateTime since);

    /**
     * 统计设备的平均辐射值
     */
    @Query("SELECT AVG(r.cpmValue) FROM RadiationDeviceStatus r WHERE r.device = :device " +
           "AND r.cpmValue IS NOT NULL AND r.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageCpmValue(@Param("device") com.ems.entity.device.Device device,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的平均电池电压
     */
    @Query("SELECT AVG(r.batteryVoltageMv) FROM RadiationDeviceStatus r WHERE r.device = :device " +
           "AND r.batteryVoltageMv IS NOT NULL AND r.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageBatteryVoltage(@Param("device") com.ems.entity.device.Device device,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的平均信号强度
     */
    @Query("SELECT AVG(r.signalQuality) FROM RadiationDeviceStatus r WHERE r.device = :device " +
           "AND r.signalQuality IS NOT NULL AND r.recordTime BETWEEN :startTime AND :endTime")
    Double getAverageSignalQuality(@Param("device") com.ems.entity.device.Device device,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查找设备健康统计
     */
    @Query("SELECT " +
           "COUNT(r) as totalRecords, " +
           "COUNT(CASE WHEN r.cpmValue > 100 THEN 1 END) as highCpmCount, " +
           "COUNT(CASE WHEN r.batteryVoltageMv < 3500 THEN 1 END) as lowBatteryCount, " +
           "COUNT(CASE WHEN r.signalQuality < 2 THEN 1 END) as weakSignalCount, " +
           "COUNT(CASE WHEN r.deviceTemperature < -20 OR r.deviceTemperature > 60 THEN 1 END) as abnormalTempCount, " +
           "COUNT(CASE WHEN r.bdsUseful = true OR r.lbsUseful = true THEN 1 END) as hasLocationCount " +
           "FROM RadiationDeviceStatus r WHERE r.device = :device " +
           "AND r.recordTime >= :since")
    Object[] getDeviceHealthStatistics(@Param("device") com.ems.entity.device.Device device,
                                     @Param("since") LocalDateTime since);

    /**
     * 清理过期数据
     */
    @Query(value = "DELETE FROM radiation_device_status WHERE record_time < :threshold", nativeQuery = true)
    int deleteOldRecords(@Param("threshold") LocalDateTime threshold);

    /**
     * 查找信号强度差的记录
     */
    @Query("SELECT r FROM RadiationDeviceStatus r WHERE r.signalQuality < 2 " +
           "AND r.recordTime >= :since ORDER BY r.recordTime DESC")
    List<RadiationDeviceStatus> findWeakSignalRecords(@Param("since") LocalDateTime since);
}