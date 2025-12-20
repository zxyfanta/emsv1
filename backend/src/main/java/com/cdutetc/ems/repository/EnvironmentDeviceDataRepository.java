package com.cdutetc.ems.repository;

import com.cdutetc.ems.entity.EnvironmentDeviceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测站数据访问接口
 */
@Repository
public interface EnvironmentDeviceDataRepository extends JpaRepository<EnvironmentDeviceData, Long> {

    /**
     * 根据设备编码查找数据
     */
    List<EnvironmentDeviceData> findByDeviceCode(String deviceCode);

    /**
     * 根据设备编码分页查询数据
     */
    Page<EnvironmentDeviceData> findByDeviceCode(String deviceCode, Pageable pageable);

    /**
     * 根据设备编码和时间范围查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceData> findByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据设备编码和时间范围分页查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    Page<EnvironmentDeviceData> findByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 查找设备最新的数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode ORDER BY e.recordTime DESC")
    Page<EnvironmentDeviceData> findLatestByDeviceCode(@Param("deviceCode") String deviceCode, Pageable pageable);

    /**
     * 查找设备最新的单条数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceData> findLatestByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 根据温度范围查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.temperature BETWEEN :minTemp AND :maxTemp")
    Page<EnvironmentDeviceData> findByTemperatureRange(@Param("minTemp") Double minTemp, @Param("maxTemp") Double maxTemp, Pageable pageable);

    /**
     * 根据设备编码和温度范围查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.temperature BETWEEN :minTemp AND :maxTemp")
    Page<EnvironmentDeviceData> findByDeviceCodeAndTemperatureRange(
            @Param("deviceCode") String deviceCode,
            @Param("minTemp") Double minTemp,
            @Param("maxTemp") Double maxTemp,
            Pageable pageable);

    /**
     * 根据湿度范围查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.wetness BETWEEN :minHumidity AND :maxHumidity")
    Page<EnvironmentDeviceData> findByHumidityRange(@Param("minHumidity") Double minHumidity, @Param("maxHumidity") Double maxHumidity, Pageable pageable);

    /**
     * 根据时间范围查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    Page<EnvironmentDeviceData> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 统计设备的数据条数
     */
    @Query("SELECT COUNT(e) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    long countByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 统计时间范围内的数据条数
     */
    @Query("SELECT COUNT(e) FROM EnvironmentDeviceData e WHERE e.recordTime BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询温度平均值
     */
    @Query("SELECT AVG(e.temperature) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageTemperatureByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询湿度平均值
     */
    @Query("SELECT AVG(e.wetness) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageHumidityByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询风速平均值
     */
    @Query("SELECT AVG(e.windspeed) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageWindSpeedByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询综合环境指数平均值
     */
    @Query("SELECT AVG(e.total) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageEnvironmentIndexByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询温度最大值
     */
    @Query("SELECT MAX(e.temperature) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findMaxTemperatureByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询温度最小值
     */
    @Query("SELECT MIN(e.temperature) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findMinTemperatureByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询CPM平均值
     */
    @Query("SELECT AVG(e.cpm) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageCpmByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均电池电压
     */
    @Query("SELECT AVG(e.battery) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Double findAverageBatteryByDeviceCodeAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ===== 企业相关方法 =====

    /**
     * 根据企业ID分页查询数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Page<EnvironmentDeviceData> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * 根据设备编码按记录时间降序分页查询
     */
    Page<EnvironmentDeviceData> findByDeviceCodeOrderByRecordTimeDesc(String deviceCode, Pageable pageable);

    /**
     * 根据设备编码和时间范围按记录时间降序分页查询
     */
    Page<EnvironmentDeviceData> findByDeviceCodeAndRecordTimeBetweenOrderByRecordTimeDesc(
            String deviceCode, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据企业ID和时间范围按记录时间降序分页查询
     */
    @Query("SELECT e FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime DESC")
    Page<EnvironmentDeviceData> findByCompanyIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 获取设备最新单条数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceData> findTopByDeviceCodeOrderByRecordTimeDesc(@Param("deviceCode") String deviceCode);

    /**
     * 获取企业最新单条数据
     */
    @Query("SELECT e FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId ORDER BY e.recordTime DESC")
    List<EnvironmentDeviceData> findTopByCompanyIdOrderByRecordTimeDesc(@Param("companyId") Long companyId);

    /**
     * 统计企业数据条数
     */
    @Query("SELECT COUNT(e) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * 统计企业在指定时间范围内的数据条数
     */
    @Query("SELECT COUNT(e) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    long countByCompanyIdAndRecordTimeBetween(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计企业不同设备数量
     */
    @Query("SELECT COUNT(DISTINCT e.deviceCode) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    long countDistinctDeviceCodeByCompanyId(@Param("companyId") Long companyId);

    // ===== CPM 统计方法 =====

    /**
     * 获取CPM统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(e.cpm), MIN(e.cpm), MAX(e.cpm) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getCpmStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业CPM统计信息
     */
    @Query("SELECT AVG(e.cpm), MIN(e.cpm), MAX(e.cpm) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getCpmStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的CPM统计信息
     */
    @Query("SELECT AVG(e.cpm), MIN(e.cpm), MAX(e.cpm) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getCpmStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的CPM统计信息
     */
    @Query("SELECT AVG(e.cpm), MIN(e.cpm), MAX(e.cpm) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    Object[] getCpmStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    // ===== 温度统计方法 =====

    /**
     * 获取温度统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(e.temperature), MIN(e.temperature), MAX(e.temperature) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getTemperatureStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业温度统计信息
     */
    @Query("SELECT AVG(e.temperature), MIN(e.temperature), MAX(e.temperature) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getTemperatureStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的温度统计信息
     */
    @Query("SELECT AVG(e.temperature), MIN(e.temperature), MAX(e.temperature) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getTemperatureStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的温度统计信息
     */
    @Query("SELECT AVG(e.temperature), MIN(e.temperature), MAX(e.temperature) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    Object[] getTemperatureStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    // ===== 湿度统计方法 =====

    /**
     * 获取湿度统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(e.wetness), MIN(e.wetness), MAX(e.wetness) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getHumidityStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业湿度统计信息
     */
    @Query("SELECT AVG(e.wetness), MIN(e.wetness), MAX(e.wetness) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getHumidityStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的湿度统计信息
     */
    @Query("SELECT AVG(e.wetness), MIN(e.wetness), MAX(e.wetness) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getHumidityStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的湿度统计信息
     */
    @Query("SELECT AVG(e.wetness), MIN(e.wetness), MAX(e.wetness) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    Object[] getHumidityStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    // ===== 风速统计方法 =====

    /**
     * 获取风速统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(e.windspeed), MIN(e.windspeed), MAX(e.windspeed) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getWindSpeedStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业风速统计信息
     */
    @Query("SELECT AVG(e.windspeed), MIN(e.windspeed), MAX(e.windspeed) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getWindSpeedStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的风速统计信息
     */
    @Query("SELECT AVG(e.windspeed), MIN(e.windspeed), MAX(e.windspeed) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getWindSpeedStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的风速统计信息
     */
    @Query("SELECT AVG(e.windspeed), MIN(e.windspeed), MAX(e.windspeed) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    Object[] getWindSpeedStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    // ===== 电池统计方法 =====

    /**
     * 获取电池统计信息（平均值、最小值、最大值）
     */
    @Query("SELECT AVG(e.battery), MIN(e.battery), MAX(e.battery) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getBatteryStatisticsByTimeRange(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取企业电池统计信息
     */
    @Query("SELECT AVG(e.battery), MIN(e.battery), MAX(e.battery) FROM EnvironmentDeviceData e JOIN Device d ON e.deviceCode = d.deviceCode WHERE d.company.id = :companyId")
    Object[] getBatteryStatistics(@Param("companyId") Long companyId);

    /**
     * 获取单个设备在指定时间范围内的电池统计信息
     */
    @Query("SELECT AVG(e.battery), MIN(e.battery), MAX(e.battery) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    Object[] getBatteryStatisticsByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取单个设备的电池统计信息
     */
    @Query("SELECT AVG(e.battery), MIN(e.battery), MAX(e.battery) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode")
    Object[] getBatteryStatisticsByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 统计单个设备在指定时间范围内的数据条数
     */
    @Query("SELECT COUNT(e) FROM EnvironmentDeviceData e WHERE e.deviceCode = :deviceCode AND e.recordTime BETWEEN :startTime AND :endTime")
    long countByDeviceCodeAndRecordTimeBetween(@Param("deviceCode") String deviceCode, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的数据
     */
    @Query("DELETE FROM EnvironmentDeviceData e WHERE e.recordTime < :dateTime AND e.deviceCode IN (SELECT d.deviceCode FROM Device d WHERE d.company.id = :companyId)")
    long deleteByRecordTimeBeforeAndCompanyId(@Param("dateTime") LocalDateTime dateTime, @Param("companyId") Long companyId);
}