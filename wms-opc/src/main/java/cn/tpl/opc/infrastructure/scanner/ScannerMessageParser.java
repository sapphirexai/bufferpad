package cn.tpl.opc.infrastructure.scanner;

import cn.tpl.opc.commons.constant.Constants;
import org.springframework.stereotype.Component;

@Component
public class ScannerMessageParser {
    public ScannerMessage parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return ScannerMessage.unknown();
        }

        String body = rawMessage.trim();
        boolean framed = false;
        // Strip only a complete outer envelope, never concatenate multiple frames.
        if (rawMessage.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))
                && rawMessage.endsWith(String.valueOf(Constants.SCANNER_MSG_ETX))) {
            body = rawMessage.substring(1, rawMessage.length() - 1).trim(); framed = true;
        } else if (rawMessage.startsWith(Constants.SCANNER_XZ_STX) && rawMessage.endsWith(Constants.SCANNER_XZ_ETX)) {
            body = rawMessage.substring(Constants.SCANNER_XZ_STX.length(), rawMessage.length() - Constants.SCANNER_XZ_ETX.length()).trim(); framed = true;
        }
        if (body.equals(Constants.SCANNER_MSG_HEART_BEAT)) return ScannerMessage.heartbeat();
        if (body.equals(Constants.SCANNER_MSG_NO_READ)) return ScannerMessage.noRead();
        if (!framed || body.isBlank() || body.length() > 255 || body.indexOf('\uFFFD') >= 0
                || body.contains(Constants.SCANNER_XZ_STX) || body.contains(Constants.SCANNER_XZ_ETX)
                || body.chars().anyMatch(Character::isISOControl)) return ScannerMessage.unknown();
        return ScannerMessage.barcode(body);
    }
}
