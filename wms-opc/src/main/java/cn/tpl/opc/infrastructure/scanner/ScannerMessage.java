package cn.tpl.opc.infrastructure.scanner;

public class ScannerMessage {
    private final ScannerMessageType type;
    private final String qrCode;

    private ScannerMessage(ScannerMessageType type, String qrCode) {
        this.type = type;
        this.qrCode = qrCode;
    }

    public static ScannerMessage barcode(String qrCode) {
        return new ScannerMessage(ScannerMessageType.BARCODE, qrCode);
    }

    public static ScannerMessage noRead() {
        return new ScannerMessage(ScannerMessageType.NO_READ, null);
    }

    public static ScannerMessage heartbeat() {
        return new ScannerMessage(ScannerMessageType.HEARTBEAT, null);
    }

    public static ScannerMessage unknown() {
        return new ScannerMessage(ScannerMessageType.UNKNOWN, null);
    }

    public ScannerMessageType getType() {
        return type;
    }

    public String getQrCode() {
        return qrCode;
    }
}
