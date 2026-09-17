package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OPC 全局配置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "OPC全局配置")
public class OpcConfigDTO extends AbsBaseDTO {
    @Schema(description = "固定ID")
    private Long id;

    @Schema(description = "缓冲垫最大使用数量")
    private Integer cushionMaxUseCount;
}
