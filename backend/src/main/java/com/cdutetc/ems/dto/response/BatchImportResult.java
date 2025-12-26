package com.cdutetc.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResult {

    private int totalCount;
    private int importedCount;
    private List<DeviceResponse> devices;
    private List<ActivationCodeResponse> activationCodes;
}
