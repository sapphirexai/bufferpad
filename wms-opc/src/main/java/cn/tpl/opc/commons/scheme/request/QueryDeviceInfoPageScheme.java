package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryDeviceInfoPageScheme extends BasePageScheme {
    @Schema(description = "设备类型")
    private Integer type;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "设备IP")
    private String ip;
}
