package cn.tpl.opc.service.impl;

import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.service.IDeviceInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 */
@Service("deviceService")
public class DeviceInfoServiceImpl implements IDeviceInfoService {
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;

    @Override
    public List<DeviceInfoEntity> listDeviceInfoByType(Integer type) {
        return deviceInfoEntityMapper.listDeviceInfoByType(type);
    }

}
