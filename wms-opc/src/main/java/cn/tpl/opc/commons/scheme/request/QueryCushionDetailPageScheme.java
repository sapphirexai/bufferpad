package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

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

    @Schema(description = "开始创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "截止创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
