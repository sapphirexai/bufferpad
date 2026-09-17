package cn.tpl.opc.infrastructure.device;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.service.INettyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceAutoConnectListenerTest {
    @Mock
    private INettyService nettyService;

    private DeviceAutoConnectListener listener;

    @Before
    public void setUp() {
        listener = new DeviceAutoConnectListener(nettyService);
    }

    @Test
    public void connectsAllConfiguredDevicesWhenApplicationIsReady() {
        when(nettyService.connectDevices(Constants.WORK_LINE_ALL))
                .thenReturn(ResultDTO.success(Collections.<DeviceInfoDTO>emptyList()));

        listener.onApplicationReady(null);

        verify(nettyService).connectDevices(Constants.WORK_LINE_ALL);
    }

    @Test
    public void onlyAutoConnectsOnceForTheSameApplicationContext() {
        when(nettyService.connectDevices(Constants.WORK_LINE_ALL))
                .thenReturn(ResultDTO.success(Collections.<DeviceInfoDTO>emptyList()));

        listener.onApplicationReady(null);
        listener.onApplicationReady(null);

        verify(nettyService, times(1)).connectDevices(Constants.WORK_LINE_ALL);
    }

    @Test
    public void doesNotBlockApplicationStartupWhenAutoConnectThrows() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(nettyService).connectDevices(Constants.WORK_LINE_ALL);

        listener.onApplicationReady(null);

        verify(nettyService).connectDevices(Constants.WORK_LINE_ALL);
    }
}
