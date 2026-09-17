package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询PLC地址配置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "分页查询PLC地址配置")
public class QueryPLCAddrPageScheme extends BasePageScheme {
    @Schema(description = "PLC设备ID")
    private Long plcId;

    @Schema(description = "扫码器设备ID")
    private Long scannerId;

    @Schema(description = "PLC地址类型")
    private Integer type;
}
