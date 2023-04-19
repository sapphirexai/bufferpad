package cn.tpl.opc.commons.scheme.base;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 * 协议基类
 */
@ToString
public abstract class AbsBaseScheme implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
