package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 * 查询设备信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "查询设备信息协议类")
public class QueryDeviceInfoScheme extends AbsBaseScheme {
    @Schema(description = "设备类型")
    private Integer type;
}
