package cn.tpl.opc.commons.dto.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * 分页基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbsBasePageDTO extends AbsBaseDTO {
    /**
     * 当前页
     */
    protected int cPage = 1;

    /**
     * 每页数据量
     */
    protected int pageSize = 10;
}
