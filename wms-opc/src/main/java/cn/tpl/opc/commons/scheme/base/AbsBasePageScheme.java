package cn.tpl.opc.commons.scheme.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 * 分页协议基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public abstract class AbsBasePageScheme extends AbsBaseScheme {
    /**
     * 当前页
     */
    @Schema(description = "当前页，默认为1")
    protected int currentPage = 1;

    /**
     * 每页数据量
     */
    @Schema(description = "每页数据量，默认为10")
    protected int pageSize = 10;
}
