package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/1/11
 * EventBus消息
 * 从PLC读Int值
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EventBusMsgReadOpenCountFromPLC extends AbsBaseDTO {
    /**
     * 缓冲垫二维码
     */
    private String qrCode;

    /**
     * 指令地址
     */
    private String address;

    /**
     * 产线
     */
    private Integer workLine;

    public EventBusMsgReadOpenCountFromPLC(String qrCode, String address, Integer workLine) {
        this.qrCode = qrCode;
        this.address = address;
        this.workLine = workLine;
    }
}
