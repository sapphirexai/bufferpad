package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PLC 地址配置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "PLC地址配置")
public class PLCAddrDTO extends AbsBaseDTO {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "PLC设备ID")
    private Long plcId;

    @Schema(description = "PLC设备名称")
    private String plcName;

    @Schema(description = "PLC寄存器地址")
    private String addr;

    @Schema(description = "地址操作类型")
    private Integer type;

    @Schema(description = "地址操作类型名称")
    private String typeName;

    @Schema(description = "扫码器设备ID")
    private Long scannerId;

    @Schema(description = "扫码器名称")
    private String scannerName;

    @Schema(description = "安装位置ID")
    private Long installPositionId;

    @Schema(description = "安装位置名称")
    private String installPositionName;
}
