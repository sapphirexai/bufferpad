package cn.tpl.opc.infrastructure.device;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.service.INettyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects every configured scanner and PLC once the Spring application is ready.
 *
 * Device connections are kept in memory, so they must be rebuilt after every backend restart.
 */
@Slf4j
@Component
public class DeviceAutoConnectListener {
    private final INettyService nettyService;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public DeviceAutoConnectListener(INettyService nettyService) {
        this.nettyService = nettyService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        try {
            ResultDTO<List<DeviceInfoDTO>> result = nettyService.connectDevices(Constants.WORK_LINE_ALL);
            if (result == null || !result.isCodeSuccess()) {
                log.warn("startup device auto-connect did not complete, message => {}",
                        result == null ? "no response" : result.getMsg());
                return;
            }
            List<DeviceInfoDTO> devices = result.getData();
            log.info("startup device auto-connect triggered, deviceCount => {}",
                    devices == null ? 0 : devices.size());
        } catch (Exception e) {
            // A failed device connection is handled by Connector; do not prevent the HTTP service from starting.
            log.error("startup device auto-connect failed", e);
        }
    }
}
