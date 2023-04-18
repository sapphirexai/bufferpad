package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 * 查询设备信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryDeviceInfoScheme extends AbsBaseScheme {
    private Integer type;
}
