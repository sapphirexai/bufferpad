package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.BasePageScheme;
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
public class QueryScanLogScheme extends BasePageScheme {
    public QueryScanLogScheme() {
        this.pageSize = 20;
    }

    @Schema(description = "二维码")
    private String qrCode;

    @Schema(description = "日志内容")
    private String msg;

    @Schema(description = "日志类型，0：普通；1：异常")
    private Short msgType;

    @Schema(description = "开始创建时间，格式为yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "截止创建时间，格式同上")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private String status;
    private String scanner;
    private String plc;
    public Long getScannerIdFilter() { return deviceId(scanner); }
    public Long getPlcIdFilter() { return deviceId(plc); }
    private Long deviceId(String value) {
        if(value==null || !value.trim().matches("[0-9]+")) return null;
        try { return Long.valueOf(value.trim()); } catch(NumberFormatException ignored) { return null; }
    }
}
