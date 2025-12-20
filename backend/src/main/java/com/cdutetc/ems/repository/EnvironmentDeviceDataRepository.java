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
}