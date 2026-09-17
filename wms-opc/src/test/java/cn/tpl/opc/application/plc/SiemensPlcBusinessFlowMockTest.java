package cn.tpl.opc.application.plc;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Siemens.SiemensS7Net;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.DeviceConnectionState;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.infrastructure.plc.PlcErrorClassifier;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IScanLogService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the production notification and dispatch services synchronously with a
 * mocked Siemens transport. Transport framing itself is covered separately by
 * SiemensS7LoopbackIntegrationTest.
 */
public class SiemensPlcBusinessFlowMockTest {
    private static final Long PLC_ID = 20L;
    private static final Long SCANNER_ID = 10L;

    private final PlcNotifyService notifyService = new PlcNotifyService();
    private final PlcCommandDispatcher dispatcher = new PlcCommandDispatcher();
    private final IScanLogService scanLogService = mock(IScanLogService.class);
    private final IOperationEventService operationEventService = mock(IOperationEventService.class);
    private final ICushionInfoService cushionInfoService = mock(ICushionInfoService.class);
    private final IPLCAddrService plcAddrService = mock(IPLCAddrService.class);
    private final DeviceInfoEntityMapper deviceInfoEntityMapper = mock(DeviceInfoEntityMapper.class);
    private final ConnectionMgr connectionMgr = mock(ConnectionMgr.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final SiemensS7Net s7Client = mock(SiemensS7Net.class);
    private final Connection connection = new Connection();

    @Before
    public void setUp() {
        connection.setId(PLC_ID);
        connection.setType(DeviceTypeEnum.SIEMENS_S7_PLC.getCode());
        connection.setName("Mock S7 PLC");
        connection.nowActive(s7Client);
        when(connectionMgr.getConnection(PLC_ID)).thenReturn(connection);

        DeviceInfoEntity scanner = new DeviceInfoEntity();
        scanner.setId(SCANNER_ID);
        scanner.setInstallSeq(1);
        scanner.setWorkLine(1);
        when(deviceInfoEntityMapper.selectByPrimaryKey(SCANNER_ID)).thenReturn(scanner);

        ReflectionTestUtils.setField(dispatcher, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(dispatcher, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(dispatcher, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(dispatcher, "cushionInfoService", cushionInfoService);
        ReflectionTestUtils.setField(dispatcher, "operationEventService", operationEventService);
        ReflectionTestUtils.setField(dispatcher, "plcErrorClassifier", new PlcErrorClassifier());

        ReflectionTestUtils.setField(notifyService, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(notifyService, "operationEventService", operationEventService);
        ReflectionTestUtils.setField(notifyService, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(notifyService, "plcAddrService", plcAddrService);
        ReflectionTestUtils.setField(notifyService, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(notifyService, "eventPublisher", eventPublisher);

        doAnswer(invocation -> {
            dispatcher.onPlcCommand((EventBusMsgPlcCmd) invocation.getArgument(0));
            return null;
        }).when(eventPublisher).publish(any());
    }

    @Test
    public void s71200ScanSuccessWritesThenReadsAndUpdatesOpenCount() {
        stubAddresses(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "DB100.DBW4",
                Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, "DB100.DBW12");
        when(s7Client.Write("DB100.DBW4", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        when(s7Client.ReadInt16("DB100.DBW12"))
                .thenReturn(OperateResultExOne.CreateSuccessResult((short) 12));
        when(cushionInfoService.modifyOpenCountByQrCode("QR-S7-1200", (short) 12)).thenReturn(true);

        notifyService.notifyScanSuccess("op-s7-1200", "QR-S7-1200", SCANNER_ID, SCANNER_ID, 1);

        InOrder order = inOrder(s7Client, cushionInfoService);
        order.verify(s7Client).Write("DB100.DBW4", (short) 1);
        order.verify(s7Client).ReadInt16("DB100.DBW12");
        order.verify(cushionInfoService).modifyOpenCountByQrCode("QR-S7-1200", (short) 12);
        assertSingleEvent(OperationEventCode.PLC_NOTIFY_SUCCEEDED, "op-s7-1200");
        assertEquals(DeviceConnectionState.ONLINE.name(), connection.getStatusCode());
    }

    @Test
    public void s71500ManualRescanUsesRescanWriteAndReadAddresses() {
        connection.setType(DeviceTypeEnum.SIEMENS_S7_PLC.getCode());
        stubAddresses(Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, "DB100.DBW8",
                Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT, "DB100.DBW14");
        when(s7Client.Write("DB100.DBW8", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        when(s7Client.ReadInt16("DB100.DBW14"))
                .thenReturn(OperateResultExOne.CreateSuccessResult((short) 7));
        when(cushionInfoService.modifyOpenCountByQrCode("QR-S7-1500", (short) 7)).thenReturn(true);

        notifyService.notifyScanSuccess("op-s7-1500", "QR-S7-1500", null, SCANNER_ID, 1);

        InOrder order = inOrder(s7Client, cushionInfoService);
        order.verify(s7Client).Write("DB100.DBW8", (short) 1);
        order.verify(s7Client).ReadInt16("DB100.DBW14");
        order.verify(cushionInfoService).modifyOpenCountByQrCode("QR-S7-1500", (short) 7);
        assertSingleEvent(OperationEventCode.PLC_NOTIFY_SUCCEEDED, "op-s7-1500");
    }

    @Test
    public void rejectedS7AddressDoesNotReadAndKeepsTransportConnected() {
        stubAddresses(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "DB999.DBW4",
                Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, "DB100.DBW12");
        when(s7Client.Write("DB999.DBW4", (short) 1))
                .thenReturn(new OperateResult(10000, "Data block does not exist / Address out of range"));

        notifyService.notifyScanSuccess("op-rejected", "QR-REJECTED", SCANNER_ID, SCANNER_ID, 1);

        verify(s7Client, never()).ReadInt16(any());
        verify(cushionInfoService, never()).modifyOpenCountByQrCode(any(), any());
        verify(s7Client, never()).ConnectClose();
        assertSingleEvent(OperationEventCode.PLC_WRITE_REJECTED, "op-rejected");
        assertEquals(DeviceConnectionState.DEGRADED.name(), connection.getStatusCode());
        assertFalse(connection.isNoPLCNet());
    }

    @Test
    public void transportFailureClosesS7ConnectionAndDoesNotRead() {
        stubAddresses(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "DB100.DBW4",
                Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, "DB100.DBW12");
        when(s7Client.Write("DB100.DBW4", (short) 1))
                .thenReturn(new OperateResult(10000, "Connection refused"));

        notifyService.notifyScanSuccess("op-offline", "QR-OFFLINE", SCANNER_ID, SCANNER_ID, 1);

        verify(s7Client, never()).ReadInt16(any());
        verify(cushionInfoService, never()).modifyOpenCountByQrCode(any(), any());
        verify(s7Client).ConnectClose();
        assertSingleEvent(OperationEventCode.PLC_WRITE_FAILED, "op-offline");
        assertEquals(DeviceConnectionState.OFFLINE.name(), connection.getStatusCode());
        assertTrue(connection.isNoPLCNet());
    }

    @Test
    public void readAddressRejectionOccursOnlyAfterSuccessfulWriteAndDoesNotUpdateCount() {
        stubAddresses(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "DB100.DBW4",
                Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, "DB999.DBW12");
        when(s7Client.Write("DB100.DBW4", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        when(s7Client.ReadInt16("DB999.DBW12"))
                .thenReturn(new OperateResultExOne<>(10000, "Data block does not exist / Address out of range"));

        notifyService.notifyScanSuccess("op-read-rejected", "QR-READ-REJECTED", SCANNER_ID, SCANNER_ID, 1);

        InOrder order = inOrder(s7Client);
        order.verify(s7Client).Write("DB100.DBW4", (short) 1);
        order.verify(s7Client).ReadInt16("DB999.DBW12");
        verify(cushionInfoService, never()).modifyOpenCountByQrCode(any(), any());
        verify(s7Client, never()).ConnectClose();

        ArgumentCaptor<OperationEventDTO> events = ArgumentCaptor.forClass(OperationEventDTO.class);
        verify(operationEventService, times(3)).publish(events.capture());
        assertEquals(OperationEventCode.PLC_NOTIFY_SUCCEEDED.name(), events.getAllValues().get(1).getCode());
        assertEquals(OperationEventCode.PLC_READ_FAILED.name(), events.getAllValues().get(2).getCode());
        assertEquals(DeviceConnectionState.DEGRADED.name(), connection.getStatusCode());
    }

    @Test
    public void negativeWordValueIsRejectedInsteadOfPersistedAsOpenCount() {
        stubAddresses(Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "DB100.DBW4",
                Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT, "DB100.DBW12");
        when(s7Client.Write("DB100.DBW4", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        when(s7Client.ReadInt16("DB100.DBW12"))
                .thenReturn(OperateResultExOne.CreateSuccessResult((short) -25536)); // WORD 40000

        notifyService.notifyScanSuccess("op-invalid-count", "QR-INVALID", SCANNER_ID, SCANNER_ID, 1);

        verify(cushionInfoService, never()).modifyOpenCountByQrCode(any(), any());
        ArgumentCaptor<OperationEventDTO> events = ArgumentCaptor.forClass(OperationEventDTO.class);
        verify(operationEventService, times(3)).publish(events.capture());
        List<OperationEventDTO> values = events.getAllValues().stream().filter(e -> !OperationEventCode.PLC_NOTIFY_PENDING.name().equals(e.getCode())).collect(java.util.stream.Collectors.toList());
        assertEquals(OperationEventCode.PLC_NOTIFY_SUCCEEDED.name(), values.get(0).getCode());
        assertEquals(OperationEventCode.PLC_READ_FAILED.name(), values.get(1).getCode());
        assertEquals("PLC开口数必须是0到32767之间的INT值", values.get(1).getMessage());
        assertEquals(DeviceConnectionState.DEGRADED.name(), connection.getStatusCode());
    }

    private void stubAddresses(Integer writeType, String writeAddress, Integer readType, String readAddress) {
        when(plcAddrService.findByTypeAndScannerId(eq(writeType), eq(SCANNER_ID)))
                .thenReturn(plcAddress(writeType, writeAddress));
        when(plcAddrService.findByTypeAndScannerId(eq(readType), eq(SCANNER_ID)))
                .thenReturn(plcAddress(readType, readAddress));
    }

    private PLCAddrEntity plcAddress(Integer type, String address) {
        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setPlcId(PLC_ID);
        entity.setScannerId(SCANNER_ID);
        entity.setType(type);
        entity.setAddr(address);
        return entity;
    }

    private void assertSingleEvent(OperationEventCode expectedCode, String operationId) {
        ArgumentCaptor<OperationEventDTO> event = ArgumentCaptor.forClass(OperationEventDTO.class);
        verify(operationEventService, org.mockito.Mockito.atLeast(2)).publish(event.capture());
        assertEquals(1, event.getAllValues().stream().filter(e -> expectedCode.name().equals(e.getCode())).count());
        assertEquals(operationId, event.getValue().getOperationId());
    }
}
