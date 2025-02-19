package cn.iocoder.yudao.module.iot.controller.admin.ota.vo.upgrade.task;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.iot.dal.dataobject.device.IotDeviceDO;
import cn.iocoder.yudao.module.iot.dal.dataobject.ota.IotOtaFirmwareDO;
import cn.iocoder.yudao.module.iot.enums.ota.IotOtaUpgradeTaskScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@Schema(description = "管理后台 - OTA升级任务创建/修改 Request VO")
public class IotOtaUpgradeTaskSaveReqVO {

    /**
     * 任务名称
     */
    @NotEmpty(message = "任务名称不能为空")
    @Schema(description = "任务名称", requiredMode = REQUIRED, example = "升级任务")
    private String name;

    /**
     * 任务描述
     */
    @Schema(description = "任务描述", example = "升级任务")
    private String description;

    /**
     * 固件编号
     * <p>
     * 关联 {@link IotOtaFirmwareDO#getId()}
     */
    @NotNull(message = "固件编号不能为空")
    @Schema(description = "固件编号", requiredMode = REQUIRED, example = "1024")
    private Long firmwareId;

    /**
     * 升级范围
     * <p>
     * 关联 {@link cn.iocoder.yudao.module.iot.enums.ota.IotOtaUpgradeTaskScopeEnum}
     */
    @NotNull(message = "升级范围不能为空")
    @InEnum(value = IotOtaUpgradeTaskScopeEnum.class)
    @Schema(description = "升级范围", requiredMode = REQUIRED, example = "1")
    private Integer scope;

    /**
     * 选中的设备编号数组
     * <p>
     * 关联 {@link IotDeviceDO#getId()}
     */
    @Schema(description = "选中的设备编号数组", requiredMode = REQUIRED, example = "[1,2,3,4]")
    private List<Long> deviceIds;

    /**
     * 选中的设备名字数组
     * <p>
     * 关联 {@link IotDeviceDO#getDeviceName()}
     */
    @Schema(description = "选中的设备名字数组", requiredMode = REQUIRED, example = "[设备1,设备2,设备3,设备4]")
    private List<String> deviceNames;

}
