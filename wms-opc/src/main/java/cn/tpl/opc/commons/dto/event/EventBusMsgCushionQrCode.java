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
 * 二维码
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class EventBusMsgCushionQrCode extends AbsBaseDTO {
    private String qrCode;

    public EventBusMsgCushionQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
