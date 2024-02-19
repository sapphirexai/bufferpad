package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/2/19
 * EventBus消息
 * 通知PLC后发送的缓冲垫信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventBusCushionAfterNotifyPLC extends AbsBaseDTO {
    /**
     * 产线
     */
    private Integer workLine;

    /**
     * 缓冲垫信息
     */
    private CushionInfoDTO cushionInfoDTO;
}
