package cn.tpl.opc.service;

import cn.tpl.opc.entity.DeviceInfoEntity;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 * 设备信息服务接口
 */
public interface IDeviceInfoService {
    /**
     * 根据类型查询设备信息列表
     *
     * @param type 类型
     * @return 设备信息列表
     */
    List<DeviceInfoEntity> listDeviceInfoByType(Integer type);
}
