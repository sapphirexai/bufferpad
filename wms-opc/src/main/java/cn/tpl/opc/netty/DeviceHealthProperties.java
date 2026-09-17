package cn.tpl.opc.netty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "device-health")
public class DeviceHealthProperties {
    @Min(1) @Max(20) private int failureThreshold = 3;
    @Valid private Map<Long, ScannerHeartbeat> scannerHeartbeats = new LinkedHashMap<>();
    @Valid private Map<Long, PlcProbe> plcProbes = new LinkedHashMap<>();

    @Data public static class ScannerHeartbeat {
        private boolean enabled;
        @Min(1) @Max(3600) private int intervalSeconds = 30;
        @Min(1) @Max(300) private int responseTimeoutSeconds = 10;
        @AssertTrue(message = "扫码器心跳等待时间必须小于发送间隔")
        public boolean isTimingValid() { return !enabled || responseTimeoutSeconds < intervalSeconds; }
    }
    @Data public static class PlcProbe {
        private boolean enabled;
        private String address;
        @Min(1) @Max(3600) private int intervalSeconds = 15;
        @AssertTrue(message = "PLC只读检测启用时必须填写确认安全的Int16地址")
        public boolean isAddressConfigured() { return !enabled || address != null && !address.isBlank(); }
    }
}
