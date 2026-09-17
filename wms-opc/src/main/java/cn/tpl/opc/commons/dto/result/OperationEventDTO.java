package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class OperationEventDTO extends AbsBaseDTO {
    private Long id;
    private String eventId;
    private String operationId;
    private String code;
    private String severity;
    private String title;
    private String message;
    private String suggestion;
    private Integer workLine;
    private Long scannerId;
    private Integer scannerSeq;
    private String scannerName;
    private String scannerIp;
    private Long plcId;
    private String plcName;
    private String plcIp;
    private Long deviceId;
    private String deviceName;
    private String qrCode;
    private String address;
    private Integer errorCode;
    private String technicalDetail;
    private Date occurredAt;
    private Integer usedCount;
    private Integer maxUseCount;
    private Boolean readExpected;
    private Short writeValue;

    public static OperationEventDTO of(OperationEventCode eventCode, Integer workLine) {
        OperationEventDTO event = new OperationEventDTO();
        event.setCode(eventCode.name());
        event.setSeverity(eventCode.getSeverity());
        event.setTitle(eventCode.getTitle());
        event.setMessage(eventCode.getDefaultMessage());
        event.setSuggestion(eventCode.getSuggestion());
        event.setWorkLine(workLine);
        event.setOccurredAt(new Date());
        return event;
    }
}
