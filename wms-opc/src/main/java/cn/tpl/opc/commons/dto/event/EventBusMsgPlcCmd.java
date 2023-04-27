package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/23
 * EventBus消息
 * PLC指令
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class EventBusMsgPlcCmd extends AbsBaseDTO {
    /**
     * 指令地址
     */
    private String address;
    /**
     * 指令
     */
    private int cmd;

    /**
     * 产线
     */
    private int workLine;

    public EventBusMsgPlcCmd(String address, int cmd, int workLine) {
        this.address = address;
        this.cmd = cmd;
        this.workLine = workLine;
    }
}
