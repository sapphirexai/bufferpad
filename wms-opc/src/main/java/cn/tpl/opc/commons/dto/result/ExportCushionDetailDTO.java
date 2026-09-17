package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import cn.tpl.opc.entity.CushionDetailEntity;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/4/16
 * 导出缓冲垫明细信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportCushionDetailDTO extends CushionDetailEntity implements Serializable {


}
