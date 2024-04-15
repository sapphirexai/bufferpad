package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 * 分页查询缓冲垫明细协议类
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "分页查询缓冲垫明细协议类")
@Data
public class QueryCushionDetailPageScheme extends BasePageScheme {
    @Schema(description = "二维码")
    private String qrCode;
}
