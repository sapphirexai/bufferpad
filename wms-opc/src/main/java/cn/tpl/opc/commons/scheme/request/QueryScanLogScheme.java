package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/11/8
 * 查询扫码日志协议类
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询扫码日志协议类")
@Data
public class QueryScanLogScheme extends AbsBaseScheme {
    @Schema(description = "二维码")
    private String qrCode;

    @Schema(description = "日志内容")
    private String msg;

    @Schema(description = "日志类型")
    private Short msgType;

    @Schema(description = "开始创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "截止创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
