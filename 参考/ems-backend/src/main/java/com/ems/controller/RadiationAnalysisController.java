package com.ems.controller;

import com.ems.annotation.OperationLogAnnotation;
import com.ems.dto.common.ApiResponse;
import com.ems.entity.OperationLog;
import com.ems.entity.User;
import com.ems.service.RadiationTrendAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 辐射分析控制器
 * 提供辐射数据分析和趋势预测的API接口
 *
 * @author EMS Team
 */
@RestController
@RequestMapping("/radiation/analysis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "辐射分析", description = "辐射数据分析和趋势相关接口")
public class RadiationAnalysisController {

    private final RadiationTrendAnalysisService radiationTrendAnalysisService;

    /**
     * 分析辐射水平变化趋势
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "分析辐射趋势", description = "分析指定设备在时间范围内的辐射水平变化趋势")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeRadiationTrend(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "数据间隔（小时）", required = true)
            @RequestParam int interval,
            @AuthenticationPrincipal User user) {

        Map<String, Object> trendAnalysis = radiationTrendAnalysisService
                .analyzeRadiationTrend(deviceId, startTime, endTime, interval);

        return ResponseEntity.ok(ApiResponse.success(trendAnalysis));
    }

    /**
     * 检测异常辐射事件
     */
    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "检测异常辐射事件", description = "检测指定设备在最近时间内的异常辐射事件")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> detectAnomalousRadiationEvents(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "检测时间范围（小时）", required = true)
            @RequestParam int hours,
            @AuthenticationPrincipal User user) {

        List<Map<String, Object>> anomalies = radiationTrendAnalysisService
                .detectAnomalousRadiationEvents(deviceId, hours);

        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    /**
     * 生成辐射水平统计报告
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "生成统计报告", description = "生成指定设备的详细辐射水平统计报告")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateRadiationStatisticsReport(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "统计天数", required = true)
            @RequestParam int days,
            @AuthenticationPrincipal User user) {

        Map<String, Object> report = radiationTrendAnalysisService
                .generateRadiationStatisticsReport(deviceId, days);

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 比较不同时间段的辐射水平
     */
    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "比较辐射水平", description = "比较同一设备不同时间段的辐射水平")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareRadiationLevels(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "时间段列表", required = true)
            @RequestBody List<Map<String, LocalDateTime>> periods,
            @AuthenticationPrincipal User user) {

        Map<String, Object> comparison = radiationTrendAnalysisService
                .compareRadiationLevels(deviceId, periods);

        return ResponseEntity.ok(ApiResponse.success(comparison));
    }

    /**
     * 获取实时辐射状态概览
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "获取概览", description = "获取设备当前的辐射状态概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRadiationOverview(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @AuthenticationPrincipal User user) {

        // 生成最近24小时的统计报告作为概览
        Map<String, Object> overview = radiationTrendAnalysisService
                .generateRadiationStatisticsReport(deviceId, 1);

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    /**
     * 获取辐射风险等级评估
     */
    @GetMapping("/risk-assessment")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "风险等级评估", description = "评估设备当前的辐射风险等级")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assessRadiationRisk(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "评估时间范围（小时）", required = true)
            @RequestParam int hours,
            @AuthenticationPrincipal User user) {

        // 检测异常事件作为风险评估的基础
        List<Map<String, Object>> anomalies = radiationTrendAnalysisService
                .detectAnomalousRadiationEvents(deviceId, hours);

        // 生成简要统计报告
        Map<String, Object> statistics = radiationTrendAnalysisService
                .generateRadiationStatisticsReport(deviceId, Math.max(1, hours / 24));

        // 计算风险等级
        String riskLevel = calculateRiskLevel(anomalies, statistics);

        Map<String, Object> riskAssessment = Map.of(
                "deviceId", deviceId,
                "assessmentTime", LocalDateTime.now(),
                "timeRangeHours", hours,
                "riskLevel", riskLevel,
                "anomalyCount", anomalies.size(),
                "anomalies", anomalies,
                "statistics", statistics,
                "recommendations", generateRiskRecommendations(riskLevel, anomalies.size())
        );

        return ResponseEntity.ok(ApiResponse.success(riskAssessment));
    }

    /**
     * 获取辐射数据质量报告
     */
    @GetMapping("/data-quality")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER')")
    @Operation(summary = "数据质量报告", description = "评估辐射数据的质量和完整性")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataQualityReport(
            @Parameter(description = "设备ID", required = true)
            @RequestParam String deviceId,
            @Parameter(description = "评估天数", required = true)
            @RequestParam int days,
            @AuthenticationPrincipal User user) {

        Map<String, Object> statistics = radiationTrendAnalysisService
                .generateRadiationStatisticsReport(deviceId, days);

        Map<String, Object> dataQuality = (Map<String, Object>) statistics.get("dataQuality");

        // 生成数据质量评估
        String qualityGrade = assessDataQualityGrade(dataQuality);

        Map<String, Object> qualityReport = Map.of(
                "deviceId", deviceId,
                "assessmentTime", LocalDateTime.now(),
                "evaluationPeriod", Map.of("days", days),
                "qualityMetrics", dataQuality,
                "overallGrade", qualityGrade,
                "issues", identifyDataQualityIssues(dataQuality),
                "recommendations", generateDataQualityRecommendations(qualityGrade, dataQuality)
        );

        return ResponseEntity.ok(ApiResponse.success(qualityReport));
    }

    /**
     * 计算风险等级
     */
    private String calculateRiskLevel(List<Map<String, Object>> anomalies, Map<String, Object> statistics) {
        int anomalyCount = anomalies.size();

        // 检查是否有严重异常
        boolean hasCriticalAnomalies = anomalies.stream()
                .anyMatch(anomaly -> "critical".equals(anomaly.get("severity")));

        if (hasCriticalAnomalies) {
            return "critical";
        }

        // 检查是否有高风险异常
        boolean hasHighAnomalies = anomalies.stream()
                .anyMatch(anomaly -> "high".equals(anomaly.get("severity")));

        if (hasHighAnomalies) {
            return "high";
        }

        // 根据异常数量判断
        if (anomalyCount >= 5) {
            return "medium";
        } else if (anomalyCount >= 1) {
            return "low";
        } else {
            return "normal";
        }
    }

    /**
     * 生成风险建议
     */
    private List<String> generateRiskRecommendations(String riskLevel, int anomalyCount) {
        List<String> recommendations = new ArrayList<>();

        switch (riskLevel) {
            case "critical":
                recommendations.add("立即检查设备状态，确认测量准确性");
                recommendations.add("通知安全主管，启动应急响应程序");
                recommendations.add("考虑疏散相关区域人员");
                break;
            case "high":
                recommendations.add("增加监测频率，每小时记录一次数据");
                recommendations.add("检查设备校准状态");
                recommendations.add("通知安全管理人员进行现场评估");
                break;
            case "medium":
                recommendations.add("继续监测，记录异常事件详情");
                recommendations.add("安排设备维护和校准");
                recommendations.add("分析异常模式，查找可能原因");
                break;
            case "low":
                recommendations.add("保持正常监测频率");
                recommendations.add("记录异常事件以供未来参考");
                break;
            case "normal":
                recommendations.add("辐射水平正常，继续例行监测");
                recommendations.add("定期检查设备运行状态");
                break;
        }

        if (anomalyCount > 0) {
            recommendations.add("分析异常事件的时间模式和可能原因");
        }

        return recommendations;
    }

    /**
     * 评估数据质量等级
     */
    private String assessDataQualityGrade(Map<String, Object> dataQuality) {
        if (dataQuality == null) {
            return "unknown";
        }

        Double completeness = (Double) dataQuality.get("completeness");
        Double validity = (Double) dataQuality.get("validity");

        if (completeness == null || validity == null) {
            return "unknown";
        }

        double avgQuality = (completeness + validity) / 2.0;

        if (avgQuality >= 95) {
            return "excellent";
        } else if (avgQuality >= 85) {
            return "good";
        } else if (avgQuality >= 70) {
            return "fair";
        } else if (avgQuality >= 50) {
            return "poor";
        } else {
            return "very_poor";
        }
    }

    /**
     * 识别数据质量问题
     */
    private List<String> identifyDataQualityIssues(Map<String, Object> dataQuality) {
        List<String> issues = new ArrayList<>();

        if (dataQuality == null) {
            issues.add("无法获取数据质量指标");
            return issues;
        }

        Double completeness = (Double) dataQuality.get("completeness");
        Double validity = (Double) dataQuality.get("validity");

        if (completeness != null && completeness < 90) {
            issues.add("数据完整性不足，可能存在数据丢失");
        }

        if (validity != null && validity < 95) {
            issues.add("数据有效性较低，存在无效或异常数据点");
        }

        if (completeness != null && completeness < 50) {
            issues.add("数据严重缺失，建议检查设备连接状态");
        }

        return issues;
    }

    /**
     * 生成数据质量改进建议
     */
    private List<String> generateDataQualityRecommendations(String qualityGrade, Map<String, Object> dataQuality) {
        List<String> recommendations = new ArrayList<>();

        switch (qualityGrade) {
            case "very_poor":
                recommendations.add("立即检查设备电源和网络连接");
                recommendations.add("验证设备传感器是否正常工作");
                recommendations.add("检查数据传输链路是否畅通");
                recommendations.add("考虑重启设备或更换传感器");
                break;
            case "poor":
                recommendations.add("增加数据采集频率");
                recommendations.add("检查设备校准状态");
                recommendations.add("优化数据传输协议");
                break;
            case "fair":
                recommendations.add("定期维护设备");
                recommendations.add("监控数据传输稳定性");
                recommendations.add("考虑升级设备固件");
                break;
            case "good":
                recommendations.add("保持当前的监测策略");
                recommendations.add("定期检查设备状态");
                break;
            case "excellent":
                recommendations.add("数据质量优秀，继续保持");
                recommendations.add("考虑进行数据分析优化");
                break;
        }

        return recommendations;
    }
}