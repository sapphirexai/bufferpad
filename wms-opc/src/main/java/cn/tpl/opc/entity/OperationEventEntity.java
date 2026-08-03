package cn.tpl.opc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("operation_event")
public class OperationEventEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private Long deviceId;
    private String deviceName;
    private String qrCode;
    private String address;
    private Integer errorCode;
    private String technicalDetail;
    private Date createdDate;
}
