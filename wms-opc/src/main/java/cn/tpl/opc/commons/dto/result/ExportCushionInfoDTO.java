package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/1/12
 * 导出缓冲垫信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportCushionInfoDTO extends AbsBaseDTO {

    /**
     * 缓冲垫二维码
     */
    @ExcelProperty("二维码")
    private String qrCode;

    /**
     * 最大使用次数
     */
    @ExcelProperty("最大使用次数")
    private Integer maxUseCount;

    /**
     * 已使用次数
     */
    @ExcelProperty("已使用次数")
    private Integer usedCount;

    /**
     * 开口数
     */
    @ExcelProperty("开口数")
    private Short openCount;

    /**
     * 扫码器安装顺序
     */
    @ExcelProperty("位置")
    private String position;

    /**
     * 最近一次扫码时间
     */
    @ExcelProperty("最近一次扫码时间")
    private String lastScanDate;

    /**
     * 创建时间
     */
    @ExcelProperty("创建时间")
    private String createdDate;
}
