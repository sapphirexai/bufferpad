package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IScanLogService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlcNotifyServiceTest {
    private final PlcNotifyService service = new PlcNotifyService();
    private final IScanLogService scanLogService = mock(IScanLogService.class);
    private final DeviceInfoEntityMapper deviceInfoEntityMapper = mock(DeviceInfoEntityMapper.class);
    private final IPLCAddrService plcAddrService = mock(IPLCAddrService.class);
    private final ConnectionMgr connectionMgr = mock(ConnectionMgr.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(service, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(service, "plcAddrService", plcAddrService);
        ReflectionTestUtils.setField(service, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
    }

    @Test
    public void invalidScannerScanPublishesSuccessInsteadOfFailure() {
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS), eq(10L)))
                .thenReturn(plcAddr(99L, "D100", Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, 10L));
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT), eq(10L)))
                .thenReturn(plcAddr(99L, "D110", Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, 10L));

        service.notifyInvalidScan("QR-001", 10L, 10L, 1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());

        EventBusMsgPlcCmd cmd = (EventBusMsgPlcCmd) captor.getAllValues().get(0);
        Assert.assertEquals(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, cmd.getAddrType().intValue());
        Assert.assertEquals(Integer.valueOf(1), cmd.getWorkLine());

        EventBusMsgReadOpenCountFromPLC read = (EventBusMsgReadOpenCountFromPLC) captor.getAllValues().get(1);
        Assert.assertEquals(Integer.valueOf(1), read.getWorkLine());
    }

    @Test
    public void manualAllLineScanUsesScannerWorkLineForPlcEvent() {
        DeviceInfoEntity scanner = new DeviceInfoEntity();
        scanner.setId(10L);
        scanner.setWorkLine(2);
        when(deviceInfoEntityMapper.selectByPrimaryKey(10L)).thenReturn(scanner);
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS), eq(10L)))
                .thenReturn(plcAddr(99L, "D200", Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, 10L));
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT), eq(10L)))
                .thenReturn(plcAddr(99L, "D210", Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT, 10L));

        service.notifyInvalidScan("QR-002", null, 10L, Constants.WORK_LINE_ALL);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());

        EventBusMsgPlcCmd cmd = (EventBusMsgPlcCmd) captor.getAllValues().get(0);
        Assert.assertEquals(Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, cmd.getAddrType().intValue());
        Assert.assertEquals(Integer.valueOf(2), cmd.getWorkLine());

        EventBusMsgReadOpenCountFromPLC read = (EventBusMsgReadOpenCountFromPLC) captor.getAllValues().get(1);
        Assert.assertEquals(Integer.valueOf(2), read.getWorkLine());
    }

    private PLCAddrEntity plcAddr(Long plcId, String addr, Integer type, Long scannerId) {
        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setPlcId(plcId);
        entity.setAddr(addr);
        entity.setType(type);
        entity.setScannerId(scannerId);
        return entity;
    }
}
