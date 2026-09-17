package cn.tpl.opc.application.scan;

import cn.tpl.opc.util.OperationIdUtils;

public class ScanCommand {
    private final String operationId;
    private final Long scannerId;
    private final Integer workLine;
    private final String scannerHost;
    private final String scannerName;
    private final String scannerPosition;
    private final Integer scannerSeq;
    private final String qrCode;

    public ScanCommand(Long scannerId, Integer workLine, String scannerHost, String scannerName, String scannerPosition, Integer scannerSeq, String qrCode) {
        this(null, scannerId, workLine, scannerHost, scannerName, scannerPosition, scannerSeq, qrCode);
    }

    public ScanCommand(String operationId, Long scannerId, Integer workLine, String scannerHost, String scannerName,
                       String scannerPosition, Integer scannerSeq, String qrCode) {
        this.operationId = OperationIdUtils.ensure(operationId);
        this.scannerId = scannerId;
        this.workLine = workLine;
        this.scannerHost = scannerHost;
        this.scannerName = scannerName;
        this.scannerPosition = scannerPosition;
        this.scannerSeq = scannerSeq;
        this.qrCode = qrCode;
    }

    public String getOperationId() {
        return operationId;
    }

    public Long getScannerId() {
        return scannerId;
    }

    public Integer getWorkLine() {
        return workLine;
    }

    public String getScannerHost() {
        return scannerHost;
    }

    public String getScannerName() {
        return scannerName;
    }

    public String getScannerPosition() {
        return scannerPosition;
    }

    public Integer getScannerSeq() {
        return scannerSeq;
    }

    public String getQrCode() {
        return qrCode;
    }
}
