package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/4/16
 * 导出缓冲垫明细信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportCushionDetailDTO extends AbsBaseDTO {

    /**
     * 缓冲垫二维码
     */
    @ExcelProperty("二维码")
    private String qrCode;

    /**
     * 开口数
     */
    @ExcelProperty("开口数")
    private Short openCount;

    /**
     * 扫码器安装顺序
     */
    @ExcelProperty("位置")
    private String scannerPosition;

    /**
     * 扫码时间
     */
    @ExcelProperty("扫码时间")
    private String createdDate;
}
