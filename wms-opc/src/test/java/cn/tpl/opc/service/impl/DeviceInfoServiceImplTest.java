package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.scheme.request.SaveDeviceInfoScheme;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.DeviceInstallPositionEntity;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.mapper.PLCAddrEntityMapper;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceInfoServiceImplTest {
    private final DeviceInfoEntityMapper deviceMapper = mock(DeviceInfoEntityMapper.class);
    private final PLCAddrEntityMapper plcAddrMapper = mock(PLCAddrEntityMapper.class);
    private final IDeviceInstallPositionService positionService = mock(IDeviceInstallPositionService.class);
    private final DeviceInfoServiceImpl service = new DeviceInfoServiceImpl();

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "deviceInfoEntityMapper", deviceMapper);
        ReflectionTestUtils.setField(service, "plcAddrEntityMapper", plcAddrMapper);
        ReflectionTestUtils.setField(service, "installPositionService", positionService);
        when(positionService.findEntityById(1L)).thenReturn(new DeviceInstallPositionEntity());
        when(positionService.getNameById(1)).thenReturn("PLC柜");
        when(deviceMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(plcAddrMapper.countByPlcId(20L)).thenReturn(1);
    }

    @Test
    public void allowsSwitchingBetweenS7ModelsWithoutDeletingCompatibleAddresses() {
        when(deviceMapper.selectByPrimaryKey(20L))
                .thenReturn(device(DeviceTypeEnum.SIEMENS_S7_1200_PLC.getCode()));

        assertTrue(service.save(scheme(DeviceTypeEnum.SIEMENS_S7_1500_PLC.getCode())));

        verify(deviceMapper).updateByPrimaryKeySelective(any(DeviceInfoEntity.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void blocksChangingProtocolFamilyWhileAddressesReferenceThePlc() {
        when(deviceMapper.selectByPrimaryKey(20L))
                .thenReturn(device(DeviceTypeEnum.MITSUBISHI_PLC.getCode()));

        service.save(scheme(DeviceTypeEnum.SIEMENS_S7_1200_PLC.getCode()));
    }

    private SaveDeviceInfoScheme scheme(Integer type) {
        SaveDeviceInfoScheme scheme = new SaveDeviceInfoScheme();
        scheme.setId(20L);
        scheme.setType(type);
        scheme.setName("1线主PLC");
        scheme.setIp("192.0.2.12");
        scheme.setPort(102);
        scheme.setInstallSeq(1);
        scheme.setWorkLine(1);
        return scheme;
    }

    private DeviceInfoEntity device(Integer type) {
        DeviceInfoEntity entity = new DeviceInfoEntity();
        entity.setId(20L);
        entity.setType(type);
        entity.setInstallSeq(1);
        entity.setWorkLine(1);
        return entity;
    }
}
