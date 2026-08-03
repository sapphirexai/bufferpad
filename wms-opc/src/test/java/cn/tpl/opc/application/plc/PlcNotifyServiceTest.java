package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IScanLogService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlcNotifyServiceTest {
    private final PlcNotifyService service = new PlcNotifyService();
    private final IScanLogService scanLogService = mock(IScanLogService.class);
    private final DeviceInfoEntityMapper deviceInfoEntityMapper = mock(DeviceInfoEntityMapper.class);
    private final IPLCAddrService plcAddrService = mock(IPLCAddrService.class);
    private final ConnectionMgr connectionMgr = mock(ConnectionMgr.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final IOperationEventService operationEventService = mock(IOperationEventService.class);

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(service, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(service, "plcAddrService", plcAddrService);
        ReflectionTestUtils.setField(service, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(service, "operationEventService", operationEventService);
        Connection connection = mock(Connection.class);
        when(connection.isDead()).thenReturn(false);
        when(connection.isNoPLCNet()).thenReturn(false);
        when(connectionMgr.getConnection(99L)).thenReturn(connection);
    }

    @Test
    public void invalidScannerScanPublishesSuccessInsteadOfFailure() {
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS), eq(10L)))
                .thenReturn(plcAddr(99L, "D100", Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, 10L));
        when(plcAddrService.findByTypeAndScannerId(eq(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT), eq(10L)))
                .thenReturn(plcAddr(99L, "D110", Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, 10L));

        service.notifyInvalidScan("QR-001", 10L, 10L, 1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());

        EventBusMsgPlcCmd cmd = (EventBusMsgPlcCmd) captor.getValue();
        Assert.assertEquals(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, cmd.getAddrType().intValue());
        Assert.assertEquals(Integer.valueOf(1), cmd.getWorkLine());
        Assert.assertEquals("D110", cmd.getReadAddress());
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
        verify(eventPublisher).publish(captor.capture());

        EventBusMsgPlcCmd cmd = (EventBusMsgPlcCmd) captor.getValue();
        Assert.assertEquals(Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, cmd.getAddrType().intValue());
        Assert.assertEquals(Integer.valueOf(2), cmd.getWorkLine());
        Assert.assertEquals("D210", cmd.getReadAddress());
    }

    @Test
    public void newManualScanDoesNotBroadcastToEveryScanner() {
        DeviceInfoEntity first = new DeviceInfoEntity();
        first.setId(10L);
        first.setWorkLine(1);
        DeviceInfoEntity second = new DeviceInfoEntity();
        second.setId(11L);
        second.setWorkLine(1);
        when(deviceInfoEntityMapper.listDeviceInfoByType(0)).thenReturn(java.util.Arrays.asList(first, second));

        service.notifyScanSuccess("QR-MANUAL-NEW", null, null, 1);

        ArgumentCaptor<OperationEventDTO> captor = ArgumentCaptor.forClass(OperationEventDTO.class);
        verify(operationEventService).publish(captor.capture());
        Assert.assertEquals(OperationEventCode.PLC_TARGET_NOT_RESOLVED.name(), captor.getValue().getCode());
        Assert.assertEquals(Integer.valueOf(1), captor.getValue().getWorkLine());
        verify(deviceInfoEntityMapper).listDeviceInfoByType(0);
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
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
