package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/4/15
 * 扫码日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "扫码日志实体类")
public class ScanLogDTO extends AbsBaseDTO {
    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 缓冲垫二维码
     */
    @Schema(description = "缓冲垫二维码")
    private String qrCode;

    /**
     * 日志内容
     */
    @Schema(description = "日志内容")
    private String msg;

    /**
     * 日志类型
     */
    @Schema(description = "日志类型")
    private Short msgType;

    /**
     * 创建时间
     */
    @Schema(description = "创建该条记录的时间")
    private Date createdDate;
    private String operationId;
    private String operationType;
    private String status;
    private String resultCode;
    private Integer workLine;
    private String operatorName;
    private Long scannerId;
    private Long plcId;
    private String scannerSnapshot;
    private String plcSnapshot;
}
