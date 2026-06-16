package cn.tpl.opc.application.scan;

import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class ScanEventListener implements InitializingBean, DisposableBean {
    @Resource
    private ScanApplicationService scanApplicationService;

    @Override
    public void afterPropertiesSet() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgCushionQrCode event) {
        log.info("onMessageEvent, qrCode => {}", event);
        ScanCommand command = new ScanCommand(
                event.getScannerId(),
                event.getWorkLine(),
                event.getScannerHost(),
                event.getScannerName(),
                event.getScannerPosition(),
                event.getScannerSeq(),
                event.getQrCode()
        );

        if (StringUtils.isEmpty(event.getQrCode())) {
            scanApplicationService.handleScanCodeFailed(command);
            return;
        }
        scanApplicationService.handleScan(command);
    }
}
