package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 保存OPC全局配置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "保存OPC全局配置")
public class SaveOpcConfigScheme extends AbsBaseScheme {
    @Schema(description = "固定ID，只允许为1")
    private Long id;

    @NotNull(message = "缓冲垫最大使用数量不能为空！")
    @Schema(description = "缓冲垫最大使用数量")
    @Min(value = 1, message = "缓冲垫最大使用数量必须大于0！")
    private Integer cushionMaxUseCount;
}
