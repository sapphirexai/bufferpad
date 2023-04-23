package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 * 缓冲垫信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "缓冲垫信息实体类")
public class CushionInfoDTO extends AbsBaseDTO {
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
     * 最大使用次数
     */
    @Schema(description = "最大使用次数")
    private Integer maxUseCount;

    /**
     * 已使用次数
     */
    @Schema(description = "已使用次数")
    private Integer usedCount;

    /**
     * 最近一次扫码时间
     */
    @Schema(description = "最近一次扫码时间")
    private Date lastScanDate;

    /**
     * 创建时间
     */
    @Schema(description = "创建该条记录的时间")
    private Date createdDate;
}
