package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/1/12
 * 修改缓冲垫信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "修改缓冲垫信息协议类")
public class ModifyCushionInfoScheme extends AbsBaseScheme {
    @Schema(description = "最大使用次数")
    private Integer maxUseCount;

    @Schema(description = "要修改的缓冲垫ID列表")
    private List<Long> ids;
}
