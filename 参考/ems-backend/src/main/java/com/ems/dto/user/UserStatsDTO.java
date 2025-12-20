package com.ems.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计DTO
 *
 * @author EMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    /**
     * 总用户数
     */
    private Long total;

    /**
     * 活跃用户数
     */
    private Long active;

    /**
     * 非活跃用户数
     */
    private Long inactive;

    /**
     * 锁定用户数
     */
    private Long locked;

    /**
     * 在线用户数
     */
    private Long online;
}