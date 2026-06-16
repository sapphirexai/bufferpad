package cn.tpl.opc.infrastructure.scanner;

import cn.tpl.opc.commons.constant.Constants;
import org.junit.Assert;
import org.junit.Test;

public class ScannerMessageParserTest {
    private final ScannerMessageParser parser = new ScannerMessageParser();

    @Test
    public void parseStandardStxEtxBarcode() {
        ScannerMessage message = parser.parse(Constants.SCANNER_MSG_STX + "ABC/123" + Constants.SCANNER_MSG_ETX);

        Assert.assertEquals(ScannerMessageType.BARCODE, message.getType());
        Assert.assertEquals("ABC/123", message.getQrCode());
    }

    @Test
    public void parseTplWrappedBarcode() {
        ScannerMessage message = parser.parse(Constants.SCANNER_XZ_STX + "QR-001" + Constants.SCANNER_XZ_ETX);

        Assert.assertEquals(ScannerMessageType.BARCODE, message.getType());
        Assert.assertEquals("QR-001", message.getQrCode());
    }

    @Test
    public void parseNoRead() {
        ScannerMessage message = parser.parse(Constants.SCANNER_MSG_NO_READ);

        Assert.assertEquals(ScannerMessageType.NO_READ, message.getType());
    }

    @Test
    public void parseHeartbeat() {
        ScannerMessage message = parser.parse(Constants.SCANNER_MSG_HEART_BEAT);

        Assert.assertEquals(ScannerMessageType.HEARTBEAT, message.getType());
    }
}
