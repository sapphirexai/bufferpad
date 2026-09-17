package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/4/15
 * 缓冲垫明细信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "缓冲垫明细信息实体类")
public class CushionDetailDTO extends AbsBaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;
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
     * 开口数
     */
    @Schema(description = "开口数")
    private Short openCount;

    /**
     * 产线
     */
    @Schema(description = "产线")
    private Integer workLine;

    /**
     * 扫码器安装顺序
     */
    @Schema(description = "扫码器安装顺序")
    private Integer scannerSeq;

    @Schema(description = "扫码器设备ID")
    private Long scannerId;

    /**
     * 扫码器安装位置
     */
    @Schema(description = "扫码器安装位置")
    private String scannerPosition;


    /**
     * 创建时间
     */
    @Schema(description = "创建该条记录的时间")
    private Date createdDate;
}
