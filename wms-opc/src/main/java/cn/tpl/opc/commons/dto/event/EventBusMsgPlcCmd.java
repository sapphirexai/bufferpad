package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/23
 * EventBus消息
 * PLC指令
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EventBusMsgPlcCmd extends AbsBaseDTO {
    /**
     * 地址类型
     */
    private Integer addrType;

    /**
     * 指令地址
     */
    private String address;

    /**
     * 指令
     */
    private Short cmd;

    /**
     * 产线
     */
    private Integer workLine;

    public EventBusMsgPlcCmd(Integer addrType, String address, Short cmd, Integer workLine) {
        this.addrType = addrType;
        this.address = address;
        this.cmd = cmd;
        this.workLine = workLine;
    }
}
