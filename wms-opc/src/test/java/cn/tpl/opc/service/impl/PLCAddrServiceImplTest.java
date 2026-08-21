package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.dto.enums.PlcAddrTypeEnum;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.scheme.request.SavePLCAddrScheme;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.mapper.PLCAddrEntityMapper;
import cn.tpl.opc.service.IDeviceInfoService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PLCAddrServiceImplTest {
    private final PLCAddrEntityMapper mapper = mock(PLCAddrEntityMapper.class);
    private final IDeviceInfoService deviceInfoService = mock(IDeviceInfoService.class);
    private final PLCAddrServiceImpl service = new PLCAddrServiceImpl();

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "plcAddrEntityMapper", mapper);
        ReflectionTestUtils.setField(service, "deviceInfoService", deviceInfoService);

        DeviceInfoDTO plc = device(20L, DeviceTypeEnum.SIEMENS_S7_1200_PLC.getCode());
        DeviceInfoDTO scanner = device(10L, DeviceTypeEnum.SCANNER.getCode());
        when(deviceInfoService.findById(20L)).thenReturn(plc);
        when(deviceInfoService.findById(10L)).thenReturn(scanner);
        when(mapper.insertSelective(org.mockito.ArgumentMatchers.any())).thenReturn(1);
    }

    @Test
    public void normalizesAndSavesSiemensWordAddress() {
        SavePLCAddrScheme scheme = scheme("db1.dbw0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode());

        service.save(scheme);

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("DB1.DBW0", captor.getValue().getAddr());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSiemensBitAddressForInt16Command() {
        service.save(scheme("DB1.DBX0.0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInputWordForWriteCommand() {
        service.save(scheme("IW0", PlcAddrTypeEnum.HEART_BEAT.getCode()));
    }

    @Test
    public void acceptsInputWordForOpenCountRead() {
        service.save(scheme("IW0", PlcAddrTypeEnum.SCAN_SUCCESS_OPEN_COUNT.getCode()));

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("IW0", captor.getValue().getAddr());
    }

    @Test
    public void acceptsOutputWordForWriteCommand() {
        service.save(scheme("QW2", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("QW2", captor.getValue().getAddr());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTiaPercentPrefixNotSupportedByDriver() {
        service.save(scheme("%DB1.DBW0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDbZeroBecauseS7DataBlocksStartAtOne() {
        service.save(scheme("DB0.DBW0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDbNumberThatHslWouldSilentlyTruncate() {
        service.save(scheme("DB65536.DBW0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWordOffsetThatHslWouldSilentlyWrapToZero() {
        service.save(scheme("DB1.DBW2097152", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnparseablyLargeNumericAddress() {
        service.save(scheme("MW999999999999999999999999", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test
    public void acceptsLargestEncodableDbWordAddress() {
        service.save(scheme("DB65535.DBW2097150", PlcAddrTypeEnum.SCAN_SUCCESS_OPEN_COUNT.getCode()));

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("DB65535.DBW2097150", captor.getValue().getAddr());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSamePhysicalS7AddressUsedByAnotherOperation() {
        when(mapper.listByPlcId(20L)).thenReturn(Collections.singletonList(
                configuredAddress(1L, "DB100.DBW0")));

        service.save(scheme("DB100.DBW0", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPartiallyOverlappingS7WordAddress() {
        when(mapper.listByPlcId(20L)).thenReturn(Collections.singletonList(
                configuredAddress(1L, "DB100.DBW0")));

        service.save(scheme("DB100.DBW1", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));
    }

    @Test
    public void acceptsAdjacentNonOverlappingS7WordAddress() {
        when(mapper.listByPlcId(20L)).thenReturn(Collections.singletonList(
                configuredAddress(1L, "DB100.DBW0")));

        service.save(scheme("DB100.DBW2", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("DB100.DBW2", captor.getValue().getAddr());
    }

    @Test
    public void leavesMitsubishiAddressUnchanged() {
        when(deviceInfoService.findById(20L))
                .thenReturn(device(20L, DeviceTypeEnum.MITSUBISHI_PLC.getCode()));
        service.save(scheme("D6600", PlcAddrTypeEnum.SCAN_SUCCESS.getCode()));

        ArgumentCaptor<PLCAddrEntity> captor = ArgumentCaptor.forClass(PLCAddrEntity.class);
        verify(mapper).insertSelective(captor.capture());
        assertEquals("D6600", captor.getValue().getAddr());
    }

    private SavePLCAddrScheme scheme(String address, Integer type) {
        SavePLCAddrScheme scheme = new SavePLCAddrScheme();
        scheme.setPlcId(20L);
        scheme.setScannerId(10L);
        scheme.setAddr(address);
        scheme.setType(type);
        return scheme;
    }

    private DeviceInfoDTO device(Long id, Integer type) {
        DeviceInfoDTO device = new DeviceInfoDTO();
        device.setId(id);
        device.setType(type);
        device.setWorkLine(1);
        return device;
    }

    private PLCAddrEntity configuredAddress(Long id, String address) {
        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setId(id);
        entity.setPlcId(20L);
        entity.setScannerId(11L);
        entity.setType(PlcAddrTypeEnum.SCAN_FAILED.getCode());
        entity.setAddr(address);
        return entity;
    }
}
