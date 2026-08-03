package cn.tpl.opc.infrastructure.plc;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PlcErrorClassifier {
    private static final int HSL_CONNECT_ERROR = 10000;

    public PlcFailureType classify(int errorCode, String message) {
        if (errorCode == HSL_CONNECT_ERROR) return PlcFailureType.TRANSPORT;

        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized,
                "connection refused", "connection reset", "connection closed", "not connected",
                "socket", "timeout", "timed out", "network", "broken pipe",
                "连接失败", "连接被拒绝", "连接重置", "连接已关闭", "网络", "超时", "远程主机")) {
            return PlcFailureType.TRANSPORT;
        }
        return PlcFailureType.DEVICE_REJECTED;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }
}
