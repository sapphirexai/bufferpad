package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.DeviceConnectionState;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.infrastructure.plc.PlcErrorClassifier;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IScanLogService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlcCommandDispatcherTest {
    private final ConnectionMgr connectionMgr = mock(ConnectionMgr.class);
    private final DeviceInfoEntityMapper deviceInfoEntityMapper = mock(DeviceInfoEntityMapper.class);
    private final IScanLogService scanLogService = mock(IScanLogService.class);
    private final ICushionInfoService cushionInfoService = mock(ICushionInfoService.class);
    private final IOperationEventService operationEventService = mock(IOperationEventService.class);
    private final Connection connection = mock(Connection.class);
    private final PlcCommandDispatcher dispatcher = new PlcCommandDispatcher();

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(dispatcher, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(dispatcher, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(dispatcher, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(dispatcher, "cushionInfoService", cushionInfoService);
        ReflectionTestUtils.setField(dispatcher, "operationEventService", operationEventService);
        ReflectionTestUtils.setField(dispatcher, "plcErrorClassifier", new PlcErrorClassifier());

        when(connectionMgr.getConnection(20L)).thenReturn(connection);
        when(connection.isActive()).thenReturn(true);
        when(connection.isNoPLCNet()).thenReturn(false);
        when(connection.getId()).thenReturn(20L);
        when(connection.getName()).thenReturn("测试PLC");
        DeviceInfoEntity scanner = new DeviceInfoEntity();
        scanner.setId(10L);
        scanner.setName("1线-上扫码器");
        scanner.setIp("192.0.2.9");
        scanner.setInstallSeq(6);
        when(deviceInfoEntityMapper.selectByPrimaryKey(10L)).thenReturn(scanner);
        DeviceInfoEntity plc = new DeviceInfoEntity();
        plc.setId(20L);
        plc.setName("1线主PLC");
        plc.setIp("192.0.2.12");
        when(deviceInfoEntityMapper.selectByPrimaryKey(20L)).thenReturn(plc);
    }

    @Test
    public void error85KeepsConnectionAndMarksItDegraded() {
        when(connection.write("D6602", (short) 1)).thenReturn(PlcIoResult.failure(85, "PLC处于运行状态，不允许写入"));

        dispatcher.onPlcCommand(command(null));

        verify(connection).markDegraded(85, "PLC禁止运行中写入");
        verify(connection, never()).nowDead(any(), any());
        assertPublishedCode(OperationEventCode.PLC_WRITE_REJECTED);
    }

    @Test
    public void transportFailureClosesConnection() {
        when(connection.write("D6602", (short) 1)).thenReturn(PlcIoResult.failure(10000, "Connection refused"));

        dispatcher.onPlcCommand(command(null));

        verify(connection).nowDead("PLC网络或连接异常", 10000);
        verify(connection, never()).markDegraded(any(), any());
        assertPublishedCode(OperationEventCode.PLC_WRITE_FAILED);
    }

    @Test
    public void successfulWriteReadsOpenCountAfterward() {
        when(connection.write("D6602", (short) 1)).thenReturn(PlcIoResult.success(null));
        when(connection.readInt16("D6610")).thenReturn(PlcIoResult.success((short) 4));
        when(cushionInfoService.modifyOpenCountByQrCode("QR-1", (short) 4)).thenReturn(true);

        dispatcher.onPlcCommand(command("D6610"));

        verify(connection).write("D6602", (short) 1);
        verify(connection).readInt16("D6610");
        verify(cushionInfoService).modifyOpenCountByQrCode("QR-1", (short) 4);
        assertPublishedCode(OperationEventCode.PLC_NOTIFY_SUCCEEDED);
    }

    @Test
    public void successfulHeartbeatDoesNotHideBusinessDegradedState() {
        when(connection.getStatusCode()).thenReturn(DeviceConnectionState.DEGRADED.name());
        when(connection.write("D6600", (short) 1)).thenReturn(PlcIoResult.success(null));
        EventBusMsgPlcCmd heartbeat = new EventBusMsgPlcCmd(null, Constants.PLC_ADDR_TYPE_HEART_BEAT,
                20L, "D6600", (short) 1, 1, null, null);

        dispatcher.onPlcCommand(heartbeat);

        verify(connection).write("D6600", (short) 1);
        verify(connection, never()).markOnline(any());
        verify(operationEventService, never()).publish(any());
    }

    @Test public void readPersistenceExceptionMustFinalizeOperation() {
        when(connection.write("D6602", (short)1)).thenReturn(PlcIoResult.success(null));
        when(connection.readInt16("D6610")).thenReturn(PlcIoResult.success((short)4));
        when(cushionInfoService.modifyOpenCountByQrCode("QR-1", (short)4)).thenThrow(new IllegalStateException("database unavailable"));
        dispatcher.onPlcCommand(command("D6610"));
        assertPublishedCode(OperationEventCode.PLC_READ_FAILED);
        verify(connection, never()).nowDead(any(), any());
    }

    @Test public void zeroAndMaximumAreValidButNegativeAndNullNeverSave() {
        when(connection.write("D6602", (short)1)).thenReturn(PlcIoResult.success(null));
        when(cushionInfoService.modifyOpenCountByQrCode(any(), any())).thenReturn(true);
        for (Short value : new Short[]{0, 32767, -1, null}) {
            when(connection.readInt16("D6610")).thenReturn(PlcIoResult.success(value));
            dispatcher.onPlcCommand(command("D6610"));
        }
        verify(cushionInfoService).modifyOpenCountByQrCode("QR-1", (short)0);
        verify(cushionInfoService).modifyOpenCountByQrCode("QR-1", (short)32767);
        verify(cushionInfoService, never()).modifyOpenCountByQrCode("QR-1", (short)-1);
        verify(cushionInfoService, never()).modifyOpenCountByQrCode("QR-1", null);
    }

    @Test public void writeTimeoutNeverReadsOrReplaysBusinessCommand() {
        when(connection.write("D6602", (short)1)).thenReturn(PlcIoResult.failure(10000, "Receive timeout"));
        dispatcher.onPlcCommand(command("D6610"));
        verify(connection).recordRequestTimeout(3, 10000);
        verify(connection).write("D6602", (short)1);
        verify(connection, never()).readInt16(any());
        assertPublishedCode(OperationEventCode.PLC_WRITE_FAILED);
    }

    private EventBusMsgPlcCmd command(String readAddress) {
        return new EventBusMsgPlcCmd("op-dispatch", "QR-1", Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, 20L,
                "D6602", (short) 1, 1, 10L, readAddress);
    }

    private void assertPublishedCode(OperationEventCode expected) {
        ArgumentCaptor<OperationEventDTO> captor = ArgumentCaptor.forClass(OperationEventDTO.class);
        verify(operationEventService,org.mockito.Mockito.atLeastOnce()).publish(captor.capture());
        assertEquals(1,captor.getAllValues().stream().filter(e->expected.name().equals(e.getCode())).count());
        assertEquals("op-dispatch", captor.getValue().getOperationId());
        assertEquals("1线-上扫码器", captor.getValue().getScannerName());
        assertEquals("192.0.2.9", captor.getValue().getScannerIp());
        assertEquals("1线主PLC", captor.getValue().getPlcName());
        assertEquals("192.0.2.12", captor.getValue().getPlcIp());
        assertEquals("QR-1", captor.getValue().getQrCode());
    }
}
