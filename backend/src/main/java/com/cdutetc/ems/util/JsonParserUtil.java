package com.cdutetc.ems.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * JSON解析工具类
 * 提供类型安全的字段解析方法，支持字段级错误处理
 *
 * @author EMS Team
 */
@Slf4j
public class JsonParserUtil {

    private JsonParserUtil() {
        // 工具类，禁止实例化
    }

    // ==================== 基础类型解析方法 ====================

    /**
     * 安全解析整数字段
     *
     * @param node JSON节点
     * @param fieldName 字段名
     * @return Optional包装的整数值，解析失败返回Optional.empty()
     */
    public static Optional<Integer> parseInt(JsonNode node, String fieldName) {
        return parseField(node, fieldName, n -> {
            if (n.isInt()) {
                return n.asInt();
            } else if (n.isTextual()) {
                // 尝试从字符串解析整数
                try {
                    return Integer.parseInt(n.asText());
                } catch (NumberFormatException e) {
                    log.warn("字段 {} 的值 '{}' 无法转换为整数", fieldName, n.asText());
                    return null;
                }
            } else if (n.isNumber()) {
                // 从浮点数转换为整数
                return n.asInt();
            }
            return null;
        });
    }

    /**
     * 安全解析长整数字段
     */
    public static Optional<Long> parseLong(JsonNode node, String fieldName) {
        return parseField(node, fieldName, n -> {
            if (n.isLong()) {
                return n.asLong();
            } else if (n.isTextual()) {
                try {
                    return Long.parseLong(n.asText());
                } catch (NumberFormatException e) {
                    log.warn("字段 {} 的值 '{}' 无法转换为长整数", fieldName, n.asText());
                    return null;
                }
            } else if (n.isNumber()) {
                return n.asLong();
            }
            return null;
        });
    }

    /**
     * 安全解析双精度浮点数字段
     */
    public static Optional<Double> parseDouble(JsonNode node, String fieldName) {
        return parseField(node, fieldName, n -> {
            if (n.isDouble() || n.isFloatingPointNumber()) {
                return n.asDouble();
            } else if (n.isTextual()) {
                try {
                    return Double.parseDouble(n.asText());
                } catch (NumberFormatException e) {
                    log.warn("字段 {} 的值 '{}' 无法转换为浮点数", fieldName, n.asText());
                    return null;
                }
            } else if (n.isNumber()) {
                return n.asDouble();
            }
            return null;
        });
    }

    /**
     * 安全解析字符串字段
     */
    public static Optional<String> parseString(JsonNode node, String fieldName) {
        return parseField(node, fieldName, n -> {
            if (n.isTextual()) {
                return n.asText();
            } else if (n.isValueNode()) {
                // 将其他类型节点转为字符串
                return n.asText();
            }
            return null;
        });
    }

    /**
     * 安全解析布尔字段
     */
    public static Optional<Boolean> parseBoolean(JsonNode node, String fieldName) {
        return parseField(node, fieldName, n -> {
            if (n.isBoolean()) {
                return n.asBoolean();
            } else if (n.isTextual()) {
                String text = n.asText().toLowerCase();
                if ("true".equals(text) || "1".equals(text) || "yes".equals(text)) {
                    return true;
                } else if ("false".equals(text) || "0".equals(text) || "no".equals(text)) {
                    return false;
                }
            } else if (n.isInt()) {
                return n.asInt() != 0;
            }
            return null;
        });
    }

    // ==================== 嵌套对象解析方法 ====================

    /**
     * 安全解析嵌套对象
     *
     * @param node JSON节点
     * @param fieldName 字段名
     * @return Optional包装的JsonNode对象，解析失败返回Optional.empty()
     */
    public static Optional<JsonNode> parseObject(JsonNode node, String fieldName) {
        try {
            if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
                JsonNode fieldNode = node.get(fieldName);
                if (fieldNode.isObject() || fieldNode.isArray()) {
                    return Optional.of(fieldNode);
                } else {
                    log.warn("字段 {} 不是对象或数组类型", fieldName);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("解析对象字段 {} 失败: {}", fieldName, e.getMessage());
            return Optional.empty();
        }
    }

    // ==================== 带默认值的解析方法 ====================

    /**
     * 解析整数字段，失败时返回默认值
     */
    public static Integer parseIntOrDefault(JsonNode node, String fieldName, Integer defaultValue) {
        return parseInt(node, fieldName).orElse(defaultValue);
    }

    /**
     * 解析长整数字段，失败时返回默认值
     */
    public static Long parseLongOrDefault(JsonNode node, String fieldName, Long defaultValue) {
        return parseLong(node, fieldName).orElse(defaultValue);
    }

    /**
     * 解析双精度浮点数字段，失败时返回默认值
     */
    public static Double parseDoubleOrDefault(JsonNode node, String fieldName, Double defaultValue) {
        return parseDouble(node, fieldName).orElse(defaultValue);
    }

    /**
     * 解析字符串字段，失败时返回默认值
     */
    public static String parseStringOrDefault(JsonNode node, String fieldName, String defaultValue) {
        return parseString(node, fieldName).orElse(defaultValue);
    }

    /**
     * 解析布尔字段，失败时返回默认值
     */
    public static Boolean parseBooleanOrDefault(JsonNode node, String fieldName, Boolean defaultValue) {
        return parseBoolean(node, fieldName).orElse(defaultValue);
    }

    // ==================== 批量解析统计方法 ====================

    /**
     * 使用Builder模式进行批量解析，并统计解析结果
     *
     * @param parser 解析器Consumer
     * @return 解析统计信息
     */
    public static ParsingStats parseWithStats(Consumer<JsonParserHelper> parser) {
        JsonParserHelper helper = new JsonParserHelper();
        parser.accept(helper);
        return helper.getStats();
    }

    /**
     * 获取解析辅助器
     */
    public static JsonParserHelper createHelper() {
        return new JsonParserHelper();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 通用字段解析方法
     *
     * @param node JSON节点
     * @param fieldName 字段名
     * @param mapper 类型转换函数
     * @return Optional包装的转换结果
     */
    private static <T> Optional<T> parseField(JsonNode node, String fieldName, Function<JsonNode, T> mapper) {
        try {
            if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
                JsonNode fieldNode = node.get(fieldName);
                T result = mapper.apply(fieldNode);
                if (result != null) {
                    return Optional.of(result);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("解析字段 {} 失败: {}", fieldName, e.getMessage());
            return Optional.empty();
        }
    }

    // ==================== 内部辅助类 ====================

    /**
     * 解析统计信息
     */
    @lombok.Data
    public static class ParsingStats {
        private final int totalFields;
        private final int successFields;
        private final int failedFields;
        private final List<String> failedFieldNames;

        public ParsingStats(int totalFields, int successFields, int failedFields, List<String> failedFieldNames) {
            this.totalFields = totalFields;
            this.successFields = successFields;
            this.failedFields = failedFields;
            this.failedFieldNames = failedFieldNames;
        }

        /**
         * 判断是否所有字段都解析成功
         */
        public boolean isSuccess() {
            return failedFields == 0;
        }

        /**
         * 获取成功率
         */
        public double getSuccessRate() {
            return totalFields == 0 ? 0.0 : (double) successFields / totalFields * 100;
        }

        @Override
        public String toString() {
            return String.format("ParsingStats{total=%d, success=%d, failed=%d, successRate=%.2f%%, failedFields=%s}",
                    totalFields, successFields, failedFields, getSuccessRate(), failedFieldNames);
        }
    }

    /**
     * JSON解析辅助器
     * 支持Builder模式进行链式解析，并自动统计解析结果
     */
    public static class JsonParserHelper {
        private final List<String> failedFieldNameList = new ArrayList<>();
        private int totalFields = 0;
        private int successFields = 0;

        /**
         * 解析字段并记录统计信息
         *
         * @param fieldName 字段名
         * @param mapper 解析函数
         * @return Optional包装的解析结果
         */
        public <T> Optional<T> parse(String fieldName, Function<JsonNode, T> mapper) {
            totalFields++;
            try {
                T result = mapper.apply(null);
                if (result != null) {
                    successFields++;
                    return Optional.of(result);
                }
                failedFieldNameList.add(fieldName);
                return Optional.empty();
            } catch (Exception e) {
                failedFieldNameList.add(fieldName);
                log.warn("解析字段 {} 失败: {}", fieldName, e.getMessage());
                return Optional.empty();
            }
        }

        /**
         * 从JsonNode解析字段
         */
        public <T> Optional<T> parse(JsonNode node, String fieldName, Function<JsonNode, T> mapper) {
            totalFields++;
            try {
                if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
                    T result = mapper.apply(node.get(fieldName));
                    if (result != null) {
                        successFields++;
                        return Optional.of(result);
                    }
                }
                failedFieldNameList.add(fieldName);
                return Optional.empty();
            } catch (Exception e) {
                failedFieldNameList.add(fieldName);
                log.warn("解析字段 {} 失败: {}", fieldName, e.getMessage());
                return Optional.empty();
            }
        }

        /**
         * 获取解析统计信息
         */
        public ParsingStats getStats() {
            return new ParsingStats(totalFields, successFields, failedFieldNameList.size(),
                    new ArrayList<>(failedFieldNameList));
        }

        /**
         * 重置统计信息
         */
        public void reset() {
            totalFields = 0;
            successFields = 0;
            failedFieldNameList.clear();
        }
    }
}
