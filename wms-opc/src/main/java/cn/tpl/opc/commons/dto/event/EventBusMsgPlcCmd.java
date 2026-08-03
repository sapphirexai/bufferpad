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
    /** Correlates all outcomes produced by one scan operation. */
    private String operationId;

    /**
     * 二维码
     */
    private String qrCode;

    /**
     * 地址类型
     */
    private Integer addrType;

    /**
     * 目标PLC设备ID
     */
    private Long plcId;

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

    /** Source scanner device ID. */
    private Long scannerId;

    /** Optional address read only after this write succeeds. */
    private String readAddress;

    public EventBusMsgPlcCmd(String qrCode, Integer addrType, String address, Short cmd, Integer workLine) {
        this(qrCode, addrType, null, address, cmd, workLine);
    }

    public EventBusMsgPlcCmd(String qrCode, Integer addrType, Long plcId, String address, Short cmd, Integer workLine) {
        this(qrCode, addrType, plcId, address, cmd, workLine, null, null);
    }

    public EventBusMsgPlcCmd(String qrCode, Integer addrType, Long plcId, String address, Short cmd,
                             Integer workLine, Long scannerId, String readAddress) {
        this(null, qrCode, addrType, plcId, address, cmd, workLine, scannerId, readAddress);
    }

    public EventBusMsgPlcCmd(String operationId, String qrCode, Integer addrType, Long plcId, String address,
                             Short cmd, Integer workLine, Long scannerId, String readAddress) {
        this.operationId = operationId;
        this.qrCode = qrCode;
        this.addrType = addrType;
        this.plcId = plcId;
        this.address = address;
        this.cmd = cmd;
        this.workLine = workLine;
        this.scannerId = scannerId;
        this.readAddress = readAddress;
    }
}
