package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.RadiationDeviceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 辐射监测仪数据访问接口
 */
@Repository
public interface RadiationDeviceDataRepository extends JpaRepository<RadiationDeviceData, Long> {

    /**
     * 根据设备编码查找数据
     */
    List<RadiationDeviceData> findByDeviceCode(String deviceCode);

    /**
     * 根据设备编码分页查询数据
     */
    Page<RadiationDeviceData> findByDeviceCode(String deviceCode, Pageable pageable);

    /**
     * 根据设备编码和时间范围查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    List<RadiationDeviceData> findByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据设备编码和时间范围分页查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDeviceData> findByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 查找设备最新的数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode ORDER BY r.recordTime DESC")
    Page<RadiationDeviceData> findLatestByDeviceCode(@Param("deviceCode") String deviceCode, Pageable pageable);

    /**
     * 查找设备最新的单条数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode ORDER BY r.recordTime DESC")
    List<RadiationDeviceData> findLatestByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 根据CPM值范围查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.cpm BETWEEN :minCpm AND :maxCpm")
    Page<RadiationDeviceData> findByCpmRange(@Param("minCpm") Integer minCpm, @Param("maxCpm") Integer maxCpm, Pageable pageable);

    /**
     * 根据设备编码和CPM值范围查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.cpm BETWEEN :minCpm AND :maxCpm")
    Page<RadiationDeviceData> findByDeviceCodeAndCpmRange(
            @Param("deviceCode") String deviceCode,
            @Param("minCpm") Integer minCpm,
            @Param("maxCpm") Integer maxCpm,
            Pageable pageable);

    /**
     * 查找有BDS定位信息的数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.gpsType = 'BDS' AND r.gpsLongitude IS NOT NULL AND r.gpsLatitude IS NOT NULL")
    Page<RadiationDeviceData> findWithBdsLocation(Pageable pageable);

    /**
     * 查找有LBS定位信息的数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.gpsType = 'LBS' AND r.gpsLongitude IS NOT NULL AND r.gpsLatitude IS NOT NULL")
    Page<RadiationDeviceData> findWithLbsLocation(Pageable pageable);

    /**
     * 根据时间范围查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDeviceData> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 统计设备的数据条数
     */
    @Query("SELECT COUNT(r) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode")
    long countByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 统计时间范围内的数据条数
     */
    @Query("SELECT COUNT(r) FROM RadiationDeviceData r WHERE r.recordTime BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询CPM平均值
     */
    @Query("SELECT AVG(r.cpm) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageCpmByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询CPM最大值
     */
    @Query("SELECT MAX(r.cpm) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime")
    Integer findMaxCpmByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询CPM最小值
     */
    @Query("SELECT MIN(r.cpm) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime")
    Integer findMinCpmByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ===== 企业相关方法 =====

    /**
     * 根据企业ID分页查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Page<RadiationDeviceData> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * 根据设备编码按记录时间降序分页查询
     */
    Page<RadiationDeviceData> findByDeviceCodeOrderByRecordTimeDesc(String deviceCode, Pageable pageable);

    /**
     * 获取设备最新单条数据
     */
    @Query("SELECT r FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode ORDER BY r.recordTime DESC")
    List<RadiationDeviceData> findTopByDeviceCodeOrderByRecordTimeDesc(@Param("deviceCode") String deviceCode);

    /**
     * 获取企业最新单条数据
     */
    @Query("SELECT r FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId ORDER BY r.recordTime DESC")
    List<RadiationDeviceData> findTopByCompanyIdOrderByRecordTimeDesc(@Param("companyId") Long companyId);

    /**
     * 根据企业ID和时间范围查询数据
     */
    @Query("SELECT r FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDeviceData> findByCompanyIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 统计企业数据条数
     */
    @Query("SELECT COUNT(r) FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * 统计企业在指定时间范围内的数据条数
     */
    @Query("SELECT COUNT(r) FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND r.recordTime BETWEEN :startTime AND :endTime")
    long countByCompanyIdAndRecordTimeBetween(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计企业不同设备数量
     */
    @Query("SELECT COUNT(DISTINCT r.deviceCode) FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    long countDistinctDeviceCodeByCompanyId(@Param("companyId") Long companyId);

    /**
     * 获取CPM统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(r.cpm), MIN(r.cpm), MAX(r.cpm) FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND r.recordTime BETWEEN :startTime AND :endTime")
    Object[] getCpmStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业CPM统计信息
     */
    @Query("SELECT AVG(r.cpm), MIN(r.cpm), MAX(r.cpm) FROM RadiationDeviceData r JOIN Device d ON r.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getCpmStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的CPM统计信息
     */
    @Query("SELECT AVG(r.cpm), MIN(r.cpm), MAX(r.cpm) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime")
    Object[] getCpmStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的CPM统计信息
     */
    @Query("SELECT AVG(r.cpm), MIN(r.cpm), MAX(r.cpm) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode")
    Object[] getCpmStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 统计单个设备在指定时间范围内的数据条数
     */
    @Query("SELECT COUNT(r) FROM RadiationDeviceData r WHERE r.deviceCode = :deviceCode AND r.recordTime BETWEEN :startTime AND :endTime")
    long countByDeviceCodeAndRecordTimeBetween(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的数据
     */
    @Query("DELETE FROM RadiationDeviceData r WHERE r.recordTime < :dateTime AND r.deviceCode IN (SELECT d.deviceCode FROM Device d WHERE d.company.id = :companyId)")
    long deleteByRecordTimeBeforeAndCompanyId(@Param("dateTime") LocalDateTime dateTime, @Param("companyId") Long companyId);
}