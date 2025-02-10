package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/23
 * EventBus消息
 * 二维码
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventBusMsgCushionQrCode extends AbsBaseDTO {
    private String qrCode;
    private Integer workLine;
    private String scannerHost;
    private String scannerName;
    private Integer scannerSeq;
}
