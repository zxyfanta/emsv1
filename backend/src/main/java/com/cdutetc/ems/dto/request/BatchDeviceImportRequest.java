package com.cdutetc.ems.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量设备导入请求DTO
 */
@Data
public class BatchDeviceImportRequest {

    @NotEmpty(message = "导入列表不能为空")
    @Size(min = 1, max = 100, message = "每次最多导入100台设备")
    private List<DeviceImportItem> items;
}
