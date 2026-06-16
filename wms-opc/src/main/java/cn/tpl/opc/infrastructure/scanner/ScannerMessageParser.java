package cn.tpl.opc.infrastructure.scanner;

import cn.tpl.opc.commons.constant.Constants;
import org.springframework.stereotype.Component;

@Component
public class ScannerMessageParser {
    private static final String SCANNER_DATA_REGEX = "[\\x02-\\x03]";
    private static final String XZ_SCANNER_DATA_REGEX = "\\[TPL_(?:S|E)TX]";

    public ScannerMessage parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return ScannerMessage.unknown();
        }

        if (rawMessage.contains(Constants.SCANNER_MSG_HEART_BEAT)) {
            return ScannerMessage.heartbeat();
        }

        if (rawMessage.contains(Constants.SCANNER_MSG_NO_READ)) {
            return ScannerMessage.noRead();
        }

        if (rawMessage.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))) {
            return ScannerMessage.barcode(rawMessage.replaceAll(SCANNER_DATA_REGEX, "").trim());
        }

        if (rawMessage.startsWith(Constants.SCANNER_XZ_STX)) {
            return ScannerMessage.barcode(rawMessage.replaceAll(XZ_SCANNER_DATA_REGEX, "").trim());
        }

        return ScannerMessage.unknown();
    }
}
