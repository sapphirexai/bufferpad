package cn.tpl.opc.commons.dto.base;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/3/31
 */
@ToString
public abstract class AbsBaseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
