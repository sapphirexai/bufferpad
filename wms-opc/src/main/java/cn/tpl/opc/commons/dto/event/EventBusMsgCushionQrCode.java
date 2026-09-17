package cn.tpl.opc.commons.dto.event;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import cn.tpl.opc.util.OperationIdUtils;
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
public class EventBusMsgCushionQrCode extends AbsBaseDTO {
    private String operationId;
    private String qrCode;
    private Long scannerId;
    private Integer workLine;
    private String scannerHost;
    private String scannerName;
    private String scannerPosition;
    private Integer scannerSeq;

    public EventBusMsgCushionQrCode(String qrCode, Long scannerId, Integer workLine, String scannerHost,
                                    String scannerName, String scannerPosition, Integer scannerSeq) {
        this(null, qrCode, scannerId, workLine, scannerHost, scannerName, scannerPosition, scannerSeq);
    }

    public EventBusMsgCushionQrCode(String operationId, String qrCode, Long scannerId, Integer workLine,
                                    String scannerHost, String scannerName, String scannerPosition,
                                    Integer scannerSeq) {
        this.operationId = OperationIdUtils.ensure(operationId);
        this.qrCode = qrCode;
        this.scannerId = scannerId;
        this.workLine = workLine;
        this.scannerHost = scannerHost;
        this.scannerName = scannerName;
        this.scannerPosition = scannerPosition;
        this.scannerSeq = scannerSeq;
    }
}
