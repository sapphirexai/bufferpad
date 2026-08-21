package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 保存PLC地址配置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "保存PLC地址配置")
public class SavePLCAddrScheme extends AbsBaseScheme {
    @Schema(description = "主键ID，编辑时传入")
    private Long id;

    @NotNull(message = "PLC设备不能为空！")
    @Schema(description = "PLC设备ID")
    private Long plcId;

    @NotBlank(message = "PLC地址不能为空！")
    @Size(max = 64, message = "PLC地址长度不能超过64个字符！")
    @Schema(description = "PLC寄存器地址")
    private String addr;

    @NotNull(message = "PLC地址类型不能为空！")
    @Schema(description = "PLC地址类型")
    private Integer type;

    @NotNull(message = "扫码器不能为空！")
    @Schema(description = "扫码器设备ID")
    private Long scannerId;
}
