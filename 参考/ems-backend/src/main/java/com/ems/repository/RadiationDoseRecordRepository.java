package com.ems.repository;

import com.ems.entity.RadiationDoseRecord;
import com.ems.entity.device.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 辐射剂量记录数据访问层
 *
 * @author EMS Team
 */
@Repository
public interface RadiationDoseRecordRepository extends JpaRepository<RadiationDoseRecord, Long> {

    /**
     * 根据设备查找剂量记录
     *
     * @param device   设备
     * @param pageable 分页参数
     * @return 剂量记录分页
     */
    Page<RadiationDoseRecord> findByDevice(Device device, Pageable pageable);

    /**
     * 根据设备ID查找剂量记录
     *
     * @param deviceId 设备ID
     * @param pageable 分页参数
     * @return 剂量记录分页
     */
    Page<RadiationDoseRecord> findByDeviceDeviceId(String deviceId, Pageable pageable);

    /**
     * 根据设备和计算类型查找剂量记录
     *
     * @param device          设备
     * @param calculationType 计算类型
     * @param pageable        分页参数
     * @return 剂量记录分页
     */
    Page<RadiationDoseRecord> findByDeviceAndCalculationType(Device device,
                                                      RadiationDoseRecord.CalculationType calculationType,
                                                      Pageable pageable);

    /**
     * 根据设备ID和计算类型查找剂量记录
     *
     * @param deviceId        设备ID
     * @param calculationType  计算类型
     * @param pageable         分页参数
     * @return 剂量记录分页
     */
    Page<RadiationDoseRecord> findByDeviceDeviceIdAndCalculationType(String deviceId,
                                                                           RadiationDoseRecord.CalculationType calculationType,
                                                                           Pageable pageable);

    /**
     * 根据设备查找剂量记录（按记录时间倒序）
     *
     * @param device   设备
     * @param pageable 分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device = :device ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByDeviceOrderByRecordTimeDesc(@Param("device") Device device, Pageable pageable);

    /**
     * 根据设备ID和计算类型查找剂量记录（按记录时间倒序）
     *
     * @param deviceId        设备ID
     * @param calculationType  计算类型
     * @param pageable         分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device.deviceId = :deviceId AND r.calculationType = :calculationType ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByDeviceDeviceIdAndCalculationTypeOrderByRecordTimeDesc(
            @Param("deviceId") String deviceId,
            @Param("calculationType") RadiationDoseRecord.CalculationType calculationType,
            Pageable pageable);

    /**
     * 根据时间范围查找剂量记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 根据设备和时间范围查找剂量记录
     *
     * @param device    设备
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device = :device AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByDeviceAndRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("device") Device device,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 根据设备ID和时间范围查找剂量记录
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device.deviceId = :deviceId AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByDeviceDeviceIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("deviceId") String deviceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 查找超过阈值的剂量记录
     *
     * @param thresholdThreshold 阈值
     * @param pageable           分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.thresholdExceeded = true AND r.thresholdValue >= :threshold ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByThresholdExceededAndThresholdValueGreaterThanEqualOrderByRecordTimeDesc(
            @Param("thresholdThreshold") Double thresholdThreshold, Pageable pageable);

    /**
     * 根据计算类型查找剂量记录
     *
     * @param calculationType 计算类型
     * @param pageable        分页参数
     * @return 剂量记录分页
     */
    Page<RadiationDoseRecord> findByCalculationType(RadiationDoseRecord.CalculationType calculationType, Pageable pageable);

    /**
     * 根据计算类型查找剂量记录（按记录时间倒序）
     *
     * @param calculationType 计算类型
     * @param pageable        分页参数
     * @return 剂量记录分页
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.calculationType = :calculationType ORDER BY r.recordTime DESC")
    Page<RadiationDoseRecord> findByCalculationTypeOrderByRecordTimeDesc(
            @Param("calculationType") RadiationDoseRecord.CalculationType calculationType, Pageable pageable);

    /**
     * 查找指定时间范围内的累积剂量记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 累积剂量记录列表
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.calculationType = 'CUMULATIVE' AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    List<RadiationDoseRecord> findCumulativeDoseRecordsByRecordTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内的实时剂量率记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 实时剂量率记录列表
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.calculationType = 'REALTIME' AND r.recordTime BETWEEN :startTime AND :endTime ORDER BY r.recordTime DESC")
    List<RadiationDoseRecord> findRealtimeDoseRateRecordsByRecordTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找设备的最新剂量记录
     *
     * @param device 设备
     * @return 最新剂量记录
     */
    Optional<RadiationDoseRecord> findFirstByDeviceOrderByRecordTimeDesc(Device device);

    /**
     * 查找设备ID对应的最新剂量记录
     *
     * @param deviceId 设备ID
     * @return 最新剂量记录
     */
    Optional<RadiationDoseRecord> findFirstByDeviceDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 查找设备的最新累积剂量记录
     *
     * @param device 设备
     * @return 最新累积剂量记录
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device = :device AND r.calculationType = 'CUMULATIVE' ORDER BY r.recordTime DESC")
    Optional<RadiationDoseRecord> findFirstByDeviceAndCalculationTypeCumulativeOrderByRecordTimeDesc(@Param("device") Device device);

    /**
     * 查找设备ID对应的最新累积剂量记录
     *
     * @param deviceId 设备ID
     * @return 最新累积剂量记录
     */
    @Query("SELECT r FROM RadiationDoseRecord r WHERE r.device.deviceId = :deviceId AND r.calculationType = 'CUMULATIVE' ORDER BY r.recordTime DESC")
    Optional<RadiationDoseRecord> findFirstByDeviceDeviceIdAndCalculationTypeCumulativeOrderByRecordTimeDesc(@Param("deviceId") String deviceId);

    /**
     * 统计设备的累积剂量总量
     *
     * @param device 设备
     * @return 累积剂量总量
     */
    @Query("SELECT MAX(r.cumulativeDose) FROM RadiationDoseRecord r WHERE r.device = :device AND r.calculationType = 'CUMULATIVE'")
    Optional<Double> findMaxCumulativeDoseByDevice(@Param("device") Device device);

    /**
     * 统计设备ID对应的累积剂量总量
     *
     * @param deviceId 设备ID
     * @return 累积剂量总量
     */
    @Query("SELECT MAX(r.cumulativeDose) FROM RadiationDoseRecord r WHERE r.device.deviceId = :deviceId AND r.calculationType = 'CUMULATIVE'")
    Optional<Double> findMaxCumulativeDoseByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 删除指定时间之前的剂量记录
     *
     * @param beforeTime 时间阈值
     * @return 删除行数
     */
    @Query("DELETE FROM RadiationDoseRecord r WHERE r.recordTime < :beforeTime")
    int deleteByRecordTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除指定设备的所有剂量记录
     *
     * @param device 设备
     * @return 删除行数
     */
    @Query("DELETE FROM RadiationDoseRecord r WHERE r.device = :device")
    int deleteByDevice(@Param("device") Device device);

    /**
     * 删除指定设备ID的所有剂量记录
     *
     * @param deviceId 设备ID
     * @return 删除行数
     */
    @Query("DELETE FROM RadiationDoseRecord r WHERE r.device.deviceId = :deviceId")
    int deleteByDeviceId(@Param("deviceId") String deviceId);
}