package cn.tpl.opc.commons.dto.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * 分页数据基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbsBasePageDTO extends AbsBaseDTO {
    /**
     * 当前页
     */
    protected long currentPage = 1L;

    /**
     * 每页数据量
     */
    protected long pageSize = 10L;

    /**
     * 总页数
     */
    protected long totalPage;
}
