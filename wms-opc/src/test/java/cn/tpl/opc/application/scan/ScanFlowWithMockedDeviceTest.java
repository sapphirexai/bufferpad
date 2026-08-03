package cn.tpl.opc.application.scan;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.application.plc.PlcNotifyService;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.domain.scan.ScanPolicy;
import cn.tpl.opc.entity.CushionDetailEntity;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.OpcConfigEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageParser;
import cn.tpl.opc.mapper.CushionDetailEntityMapper;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IScanLogService;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.service.impl.CushionInfoServiceImpl;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the scan business flow with scanner, Netty connection and PLC I/O mocked.
 */
public class ScanFlowWithMockedDeviceTest {
    private static final Long SCANNER_ID = 10L;
    private static final Long PLC_ID = 20L;
    private static final Integer WORK_LINE = 1;
    private static final Integer SCANNER_SEQ = 101;
    private static final String SCANNER_POSITION = "上料扫码位";

    private final CushionInfoEntityMapper cushionInfoEntityMapper = mock(CushionInfoEntityMapper.class);
    private final CushionDetailEntityMapper cushionDetailEntityMapper = mock(CushionDetailEntityMapper.class);
    private final OpcConfigEntityMapper opcConfigEntityMapper = mock(OpcConfigEntityMapper.class);
    private final IScanLogService scanLogService = mock(IScanLogService.class);
    private final ISseService sseService = mock(ISseService.class);
    private final IPLCAddrService plcAddrService = mock(IPLCAddrService.class);
    private final DeviceInfoEntityMapper deviceInfoEntityMapper = mock(DeviceInfoEntityMapper.class);
    private final ConnectionMgr connectionMgr = mock(ConnectionMgr.class);
    private final DomainEventPublisher plcEventPublisher = mock(DomainEventPublisher.class);
    private final IOperationEventService operationEventService = mock(IOperationEventService.class);

    private final Map<String, CushionInfoEntity> cushionStore = new LinkedHashMap<>();
    private final List<CushionDetailEntity> detailStore = new ArrayList<>();
    private final AtomicLong cushionId = new AtomicLong(1);
    private final AtomicLong detailId = new AtomicLong(1);

    private DeviceInfoEntity scanner;
    private DeviceInfoEntity plc;
    private ScanEventListener scanEventListener;
    private CushionInfoServiceImpl cushionInfoService;

    @Before
    public void setUp() {
        reset(cushionInfoEntityMapper, cushionDetailEntityMapper, opcConfigEntityMapper, scanLogService,
                sseService, plcAddrService, deviceInfoEntityMapper, connectionMgr, plcEventPublisher, operationEventService);
        cushionStore.clear();
        detailStore.clear();
        cushionId.set(1);
        detailId.set(1);

        scanner = device(SCANNER_ID, Params.DEVICE_TYPE_KEY_SCANNER, "127.0.0.1", 9101, "测试扫码器", SCANNER_POSITION, WORK_LINE, SCANNER_SEQ);
        plc = device(PLC_ID, Params.DEVICE_TYPE_KEY_SL_PLC, "127.0.0.1", 6000, "测试PLC", "PLC柜", WORK_LINE, 1);

        ScanApplicationService scanApplicationService = new ScanApplicationService();
        ReflectionTestUtils.setField(scanApplicationService, "scanPolicy", new ScanPolicy());
        ReflectionTestUtils.setField(scanApplicationService, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(scanApplicationService, "sseService", sseService);
        ReflectionTestUtils.setField(scanApplicationService, "operationEventService", operationEventService);
        ReflectionTestUtils.setField(scanApplicationService, "plcNotifyService", buildPlcNotifyService());
        ReflectionTestUtils.setField(scanApplicationService, "opcConfigEntityMapper", opcConfigEntityMapper);
        ReflectionTestUtils.setField(scanApplicationService, "cushionInfoEntityMapper", cushionInfoEntityMapper);
        ReflectionTestUtils.setField(scanApplicationService, "cushionDetailEntityMapper", cushionDetailEntityMapper);

        cushionInfoService = new CushionInfoServiceImpl();
        ReflectionTestUtils.setField(cushionInfoService, "scanApplicationService", scanApplicationService);

        scanEventListener = new ScanEventListener();
        ReflectionTestUtils.setField(scanEventListener, "scanApplicationService", scanApplicationService);

        mockOpcConfig(2);
        mockCushionPersistence();
        mockPlcDeviceAndAddressData();
    }

    @Test
    public void standardStxBarcodeFromScannerCountsAndPublishesScanSuccessPlcEvents() {
        String qrCode = "BP-STX-001";

        sendScannerRawMessage(String.valueOf(Constants.SCANNER_MSG_STX) + qrCode + Constants.SCANNER_MSG_ETX);

        CushionInfoEntity cushion = cushionStore.get(qrCode);
        Assert.assertNotNull(cushion);
        Assert.assertEquals(Integer.valueOf(Constants.CUSHION_ADD_DEFAULT_USED_COUNT), cushion.getUsedCount());
        Assert.assertEquals(SCANNER_ID, cushion.getScannerId());
        Assert.assertEquals(SCANNER_SEQ, cushion.getScannerSeq());
        Assert.assertEquals(1, detailStore.size());

        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "D102", Constants.DEFAULT_2_PLC_VAL);
        Assert.assertEquals("D106", ((EventBusMsgPlcCmd) events.get(0)).getReadAddress());
        verify(sseService, atLeastOnce()).sendCushionMsg(any(CushionInfoDTO.class));
    }

    @Test
    public void tplWrappedBarcodeFromScannerCountsAndPublishesScanSuccessPlcEvents() {
        String qrCode = "BP-TPL-001";

        sendScannerRawMessage(Constants.SCANNER_XZ_STX + qrCode + Constants.SCANNER_XZ_ETX);

        Assert.assertTrue(cushionStore.containsKey(qrCode));
        Assert.assertEquals(1, detailStore.size());
        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "D102", Constants.DEFAULT_2_PLC_VAL);
        Assert.assertEquals("D106", ((EventBusMsgPlcCmd) events.get(0)).getReadAddress());
    }

    @Test
    public void noReadFromScannerDoesNotCountAndPublishesScanFailedPlcEvent() {
        sendScannerRawMessage(Constants.SCANNER_MSG_NO_READ);

        Assert.assertTrue(cushionStore.isEmpty());
        Assert.assertTrue(detailStore.isEmpty());
        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), null, Constants.PLC_ADDR_TYPE_SCAN_FAILED, "D100", Constants.DEFAULT_2_PLC_VAL);
        verify(sseService, atLeastOnce()).sendCushionMsg(any(CushionInfoDTO.class));
    }

    @Test
    public void heartbeatFromScannerOnlyResetsConnectionAndDoesNotTouchBusinessFlow() {
        sendScannerRawMessage(Constants.SCANNER_MSG_HEART_BEAT);

        Assert.assertTrue(cushionStore.isEmpty());
        Assert.assertTrue(detailStore.isEmpty());
        verifyNoInteractions(plcEventPublisher);
        verifyNoInteractions(sseService);
        verifyNoInteractions(scanLogService);
    }

    @Test
    public void manualNewScanCountsAndPublishesReScanSuccessForConfiguredScanner() {
        String qrCode = "BP-MANUAL-NEW";

        ResultDTO<CushionInfoDTO> result = cushionInfoService.onQrCodeReceived(
                null, WORK_LINE, null, null, Constants.SCANNER_POSITION_MANUAL, null, qrCode);

        Assert.assertTrue(result.isCodeSuccess());
        CushionInfoEntity cushion = cushionStore.get(qrCode);
        Assert.assertNotNull(cushion);
        Assert.assertNull(cushion.getScannerId());
        Assert.assertEquals(Constants.SCANNER_POSITION_MANUAL, cushion.getScannerPosition());

        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, "D104", Constants.DEFAULT_2_PLC_VAL);
        Assert.assertEquals("D107", ((EventBusMsgPlcCmd) events.get(0)).getReadAddress());
    }

    @Test
    public void manualExistingScanUsesOriginalScannerAndPublishesReScanSuccess() {
        String qrCode = "BP-MANUAL-EXISTING";
        preloadedCushion(qrCode, 1, 5, oldScanDate(), SCANNER_ID, SCANNER_SEQ, SCANNER_POSITION);

        ResultDTO<CushionInfoDTO> result = cushionInfoService.onQrCodeReceived(
                null, WORK_LINE, null, null, Constants.SCANNER_POSITION_MANUAL, null, qrCode);

        Assert.assertTrue(result.isCodeSuccess());
        CushionInfoEntity cushion = cushionStore.get(qrCode);
        Assert.assertEquals(Integer.valueOf(2), cushion.getUsedCount());
        Assert.assertEquals(SCANNER_ID, cushion.getScannerId());
        Assert.assertEquals(SCANNER_POSITION, cushion.getScannerPosition());

        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, "D104", Constants.DEFAULT_2_PLC_VAL);
        Assert.assertEquals("D107", ((EventBusMsgPlcCmd) events.get(0)).getReadAddress());
    }

    @Test
    public void repeatedScannerScanWithinEffectiveIntervalDoesNotIncrementAndPublishesInvalidNotification() {
        String qrCode = "BP-REPEAT-001";
        preloadedCushion(qrCode, 1, 5, new Date(), SCANNER_ID, SCANNER_SEQ, SCANNER_POSITION);

        sendScannerRawMessage(Constants.SCANNER_XZ_STX + qrCode + Constants.SCANNER_XZ_ETX);

        CushionInfoEntity cushion = cushionStore.get(qrCode);
        Assert.assertEquals(Integer.valueOf(1), cushion.getUsedCount());
        Assert.assertTrue(detailStore.isEmpty());

        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, "D102", Constants.DEFAULT_2_PLC_VAL);
        Assert.assertEquals("D106", ((EventBusMsgPlcCmd) events.get(0)).getReadAddress());
        verify(sseService, atLeastOnce()).sendFailMsg(any(), eq(Constants.RESULT_MSG_CUSHION_INVALID_SCAN));
    }

    @Test
    public void scannerScanReachingMaxCountPublishesOverMaximumPlcEvent() {
        String qrCode = "BP-MAX-001";
        preloadedCushion(qrCode, 1, 2, oldScanDate(), SCANNER_ID, SCANNER_SEQ, SCANNER_POSITION);

        sendScannerRawMessage(String.valueOf(Constants.SCANNER_MSG_STX) + qrCode + Constants.SCANNER_MSG_ETX);

        CushionInfoEntity cushion = cushionStore.get(qrCode);
        Assert.assertEquals(Integer.valueOf(2), cushion.getUsedCount());
        Assert.assertEquals(1, detailStore.size());

        List<Object> events = publishedPlcEvents();
        Assert.assertEquals(1, events.size());
        assertPlcCmd(events.get(0), qrCode, Constants.PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM, "D101", Constants.DEFAULT_2_PLC_VAL);
        verify(sseService, atLeastOnce()).sendFailMsg(any(), eq(Constants.RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX));
    }

    @Test
    public void unknownScannerMessageIsIgnored() {
        sendScannerRawMessage("UNKNOWN");

        Assert.assertTrue(cushionStore.isEmpty());
        Assert.assertTrue(detailStore.isEmpty());
        verifyNoInteractions(plcEventPublisher);
        verifyNoInteractions(sseService);
    }

    private PlcNotifyService buildPlcNotifyService() {
        PlcNotifyService service = new PlcNotifyService();
        ReflectionTestUtils.setField(service, "scanLogService", scanLogService);
        ReflectionTestUtils.setField(service, "deviceInfoEntityMapper", deviceInfoEntityMapper);
        ReflectionTestUtils.setField(service, "plcAddrService", plcAddrService);
        ReflectionTestUtils.setField(service, "connectionMgr", connectionMgr);
        ReflectionTestUtils.setField(service, "eventPublisher", plcEventPublisher);
        ReflectionTestUtils.setField(service, "operationEventService", operationEventService);
        return service;
    }

    private void mockOpcConfig(int maxUseCount) {
        OpcConfigEntity opcConfig = new OpcConfigEntity();
        opcConfig.setId(Constants.OPC_CONFIG_ID);
        opcConfig.setCushionMaxUseCount(maxUseCount);
        when(opcConfigEntityMapper.selectByPrimaryKey(Constants.OPC_CONFIG_ID)).thenReturn(opcConfig);
    }

    private void mockCushionPersistence() {
        when(cushionInfoEntityMapper.findByQrCode(any())).thenAnswer(invocation -> copy(cushionStore.get(invocation.getArgument(0))));
        when(cushionInfoEntityMapper.insertSelective(any(CushionInfoEntity.class))).thenAnswer(invocation -> {
            CushionInfoEntity record = invocation.getArgument(0);
            record.setId(cushionId.getAndIncrement());
            record.setCreatedDate(new Date());
            cushionStore.put(record.getQrCode(), copy(record));
            return 1;
        });
        when(cushionInfoEntityMapper.modifyUsedCountByQrCode(any(CushionInfoEntity.class), any(Date.class))).thenAnswer(invocation -> {
            CushionInfoEntity incoming = invocation.getArgument(0);
            Date lastScanDateBefore = invocation.getArgument(1);
            CushionInfoEntity stored = cushionStore.get(incoming.getQrCode());
            if (stored == null) return 0;
            if (stored.getLastScanDate() != null && stored.getLastScanDate().after(lastScanDateBefore)) return 0;

            stored.setUsedCount(stored.getUsedCount() + 1);
            stored.setLastScanDate(incoming.getLastScanDate());
            stored.setScannerId(incoming.getScannerId());
            stored.setScannerSeq(incoming.getScannerSeq());
            stored.setScannerPosition(incoming.getScannerPosition());
            stored.setModifiedDate(new Date());
            return 1;
        });
        when(cushionInfoEntityMapper.modifyOpenCountByQrCode(any(CushionInfoEntity.class))).thenAnswer(invocation -> {
            CushionInfoEntity incoming = invocation.getArgument(0);
            CushionInfoEntity stored = cushionStore.get(incoming.getQrCode());
            if (stored == null) return 0;
            stored.setOpenCount(incoming.getOpenCount());
            return 1;
        });
        when(cushionDetailEntityMapper.insertSelective(any(CushionDetailEntity.class))).thenAnswer(invocation -> {
            CushionDetailEntity record = invocation.getArgument(0);
            record.setId(detailId.getAndIncrement());
            detailStore.add(copy(record));
            return 1;
        });
        when(cushionDetailEntityMapper.modifyOpenCountByQrCode(any(CushionDetailEntity.class))).thenAnswer(invocation -> {
            CushionDetailEntity incoming = invocation.getArgument(0);
            for (int i = detailStore.size() - 1; i >= 0; i--) {
                CushionDetailEntity detail = detailStore.get(i);
                if (incoming.getQrCode().equals(detail.getQrCode())) {
                    detail.setOpenCount(incoming.getOpenCount());
                    return 1;
                }
            }
            return 0;
        });
    }

    private void mockPlcDeviceAndAddressData() {
        when(deviceInfoEntityMapper.selectByPrimaryKey(SCANNER_ID)).thenReturn(scanner);
        when(deviceInfoEntityMapper.selectByPrimaryKey(PLC_ID)).thenReturn(plc);
        when(deviceInfoEntityMapper.listDeviceInfoByType(Params.DEVICE_TYPE_KEY_SCANNER)).thenReturn(Collections.singletonList(scanner));

        Connection plcConnection = mock(Connection.class);
        when(plcConnection.isDead()).thenReturn(false);
        when(plcConnection.isNoPLCNet()).thenReturn(false);
        when(connectionMgr.getConnection(PLC_ID)).thenReturn(plcConnection);

        when(plcAddrService.findByTypeAndScannerId(anyInt(), anyLong())).thenAnswer(invocation -> {
            Integer type = invocation.getArgument(0);
            Long scannerId = invocation.getArgument(1);
            if (!SCANNER_ID.equals(scannerId)) return null;
            return plcAddr(type);
        });
    }

    private void sendScannerRawMessage(String rawMessage) {
        Connection scannerConnection = new Connection();
        BeanUtil.copyProperties(scanner, scannerConnection);

        DomainEventPublisher scannerEventPublisher = event -> {
            Assert.assertTrue(event instanceof EventBusMsgCushionQrCode);
            scanEventListener.onMessageEvent((EventBusMsgCushionQrCode) event);
        };
        MsgHandler msgHandler = new MsgHandler(scannerConnection, scannerEventPublisher, new ScannerMessageParser());
        msgHandler.channelRead(null, Unpooled.copiedBuffer(rawMessage, CharsetUtil.UTF_8));
    }

    private List<Object> publishedPlcEvents() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(plcEventPublisher, atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    private void assertPlcCmd(Object event, String qrCode, Integer addrType, String address, Short cmd) {
        Assert.assertTrue(event instanceof EventBusMsgPlcCmd);
        EventBusMsgPlcCmd cmdEvent = (EventBusMsgPlcCmd) event;
        Assert.assertEquals(qrCode, cmdEvent.getQrCode());
        Assert.assertEquals(addrType, cmdEvent.getAddrType());
        Assert.assertEquals(PLC_ID, cmdEvent.getPlcId());
        Assert.assertEquals(address, cmdEvent.getAddress());
        Assert.assertEquals(cmd, cmdEvent.getCmd());
        Assert.assertEquals(WORK_LINE, cmdEvent.getWorkLine());
    }

    private void preloadedCushion(String qrCode, int usedCount, int maxUseCount, Date lastScanDate,
                                  Long scannerId, Integer scannerSeq, String scannerPosition) {
        CushionInfoEntity entity = new CushionInfoEntity();
        entity.setId(cushionId.getAndIncrement());
        entity.setQrCode(qrCode);
        entity.setUsedCount(usedCount);
        entity.setMaxUseCount(maxUseCount);
        entity.setWorkLine(WORK_LINE);
        entity.setScannerId(scannerId);
        entity.setScannerSeq(scannerSeq);
        entity.setScannerPosition(scannerPosition);
        entity.setLastScanDate(lastScanDate);
        entity.setCreatedDate(oldScanDate());
        cushionStore.put(qrCode, copy(entity));
    }

    private PLCAddrEntity plcAddr(Integer type) {
        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setId(1000L + type);
        entity.setPlcId(PLC_ID);
        entity.setScannerId(SCANNER_ID);
        entity.setType(type);
        entity.setAddr("D" + (100 + type));
        return entity;
    }

    private DeviceInfoEntity device(Long id, Integer type, String ip, Integer port, String name, String position,
                                    Integer workLine, Integer installSeq) {
        DeviceInfoEntity entity = new DeviceInfoEntity();
        entity.setId(id);
        entity.setType(type);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setName(name);
        entity.setPosition(position);
        entity.setWorkLine(workLine);
        entity.setInstallSeq(installSeq);
        return entity;
    }

    private Date oldScanDate() {
        return new Date(System.currentTimeMillis() - Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS - 1000L);
    }

    private CushionInfoEntity copy(CushionInfoEntity source) {
        if (source == null) return null;
        CushionInfoEntity target = new CushionInfoEntity();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private CushionDetailEntity copy(CushionDetailEntity source) {
        if (source == null) return null;
        CushionDetailEntity target = new CushionDetailEntity();
        BeanUtil.copyProperties(source, target);
        return target;
    }
}
