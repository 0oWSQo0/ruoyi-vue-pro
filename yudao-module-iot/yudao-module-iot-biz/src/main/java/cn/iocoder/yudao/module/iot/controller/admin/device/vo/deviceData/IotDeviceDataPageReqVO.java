package cn.iocoder.yudao.module.iot.controller.admin.device.vo.deviceData;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - IoT 设备数据 Request VO")
@Data
public class IotDeviceDataPageReqVO extends PageParam {

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "177")
    private Long deviceId;

    @Schema(description = "属性标识符", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;

    @Schema(description = "属性名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "时间范围", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "请选择时间范围")
    private LocalDateTime[] times;

}