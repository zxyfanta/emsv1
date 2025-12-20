package com.ems.repository;

import com.ems.entity.device.Device;
import com.ems.entity.GpsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GPS轨迹记录数据访问接口
 *
 * @author EMS Team
 */
@Repository
public interface GpsRecordRepository extends JpaRepository<GpsRecord, Long> {

    /**
     * 根据设备ID查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID分页查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 根据设备ID和时间范围查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 根据设备ID和时间范围分页查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              Pageable pageable);

    /**
     * 查找设备的最新GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId ORDER BY gr.recordTime DESC")
    List<GpsRecord> findLatestByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 查找设备在指定时间之后的所有GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.recordTime > :sinceTime ORDER BY gr.recordTime ASC")
    List<GpsRecord> findByDeviceIdAfterTime(@Param("deviceId") String deviceId,
                                           @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * 查找指定时间范围内的所有GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.recordTime BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内的所有GPS记录（分页）
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.recordTime BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    Pageable pageable);

    /**
     * 根据定位类型查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.primaryType = :locationType ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByPrimaryLocationType(@Param("locationType") String locationType);

    /**
     * 根据定位类型分页查找GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.primaryType = :locationType ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findByPrimaryLocationType(@Param("locationType") String locationType, Pageable pageable);

    /**
     * 查找指定设备在时间范围内的GPS记录（按定位类型）
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.primaryType = :locationType " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByDeviceIdAndLocationTypeAndTimeRange(@Param("deviceId") String deviceId,
                                                             @Param("locationType") String locationType,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 地理围栏查询：查找指定矩形区域内的GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE " +
           "gr.primaryLongitude BETWEEN :minLongitude AND :maxLongitude AND " +
           "gr.primaryLatitude BETWEEN :minLatitude AND :maxLatitude " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findByBoundingBox(@Param("minLongitude") Double minLongitude,
                                     @Param("maxLongitude") Double maxLongitude,
                                     @Param("minLatitude") Double minLatitude,
                                     @Param("maxLatitude") Double maxLatitude,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 地理围栏查询（分页）：查找指定矩形区域内的GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE " +
           "gr.primaryLongitude BETWEEN :minLongitude AND :maxLongitude AND " +
           "gr.primaryLatitude BETWEEN :minLatitude AND :maxLatitude " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findByBoundingBox(@Param("minLongitude") Double minLongitude,
                                     @Param("maxLongitude") Double maxLongitude,
                                     @Param("minLatitude") Double minLatitude,
                                     @Param("maxLatitude") Double maxLatitude,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     Pageable pageable);

    /**
     * 查找设备的GPS记录总数
     */
    @Query("SELECT COUNT(gr) FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId")
    long countByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 查找设备在指定时间范围内的GPS记录总数
     */
    @Query("SELECT COUNT(gr) FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.recordTime BETWEEN :startTime AND :endTime")
    long countByDeviceIdAndTimeRange(@Param("deviceId") String deviceId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 统计不同定位类型的记录数量
     */
    @Query("SELECT gr.primaryType, COUNT(gr) FROM GpsRecord gr " +
           "WHERE gr.recordTime BETWEEN :startTime AND :endTime " +
           "GROUP BY gr.primaryType")
    List<Object[]> countByLocationTypeBetween(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的GPS记录（全局）
     */
    @Query("SELECT gr FROM GpsRecord gr ORDER BY gr.recordTime DESC")
    List<GpsRecord> findRecentRecords(Pageable pageable);

    /**
     * 根据设备查找有有效定位的GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND (gr.bdsUseful = true OR gr.lbsUseful = true) " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findValidLocationRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备查找BDS有效的GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.bdsUseful = true " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findBdsValidRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备查找LBS有效的GPS记录
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.lbsUseful = true " +
           "ORDER BY gr.recordTime DESC")
    List<GpsRecord> findLbsValidRecordsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 删除指定时间之前的GPS记录（数据清理）
     */
    @Query("DELETE FROM GpsRecord gr WHERE gr.recordTime < :thresholdDate")
    void deleteByRecordTimeBefore(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * 查找设备在指定时间的GPS记录位置（用于轨迹回放）
     */
    @Query("SELECT gr FROM GpsRecord gr WHERE gr.device.deviceId = :deviceId " +
           "AND gr.recordTime <= :targetTime " +
           "ORDER BY gr.recordTime DESC")
    Page<GpsRecord> findLocationAtOrBeforeTime(@Param("deviceId") String deviceId,
                                             @Param("targetTime") LocalDateTime targetTime,
                                             Pageable pageable);
}