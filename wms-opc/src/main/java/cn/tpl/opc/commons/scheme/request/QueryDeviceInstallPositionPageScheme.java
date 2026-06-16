package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询设备安装位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "分页查询设备安装位置")
public class QueryDeviceInstallPositionPageScheme extends BasePageScheme {
    @Schema(description = "位置名称")
    private String name;
}
