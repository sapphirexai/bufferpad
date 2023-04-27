package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
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

    /**
     * 分页查询
     *
     * @param scheme 分页查询参数协议
     * @return 分页数据
     */
    PageData<DeviceInfoDTO> listByPage(BasePageScheme scheme);

    /**
     * 查询所有设备数据
     *
     * @return 所有设备数据
     */
    List<DeviceInfoEntity> list();

    /**
     * 根据产线查询所有设备数据
     *
     * @param workLine 产线
     * @return 所有设备数据
     */
    List<DeviceInfoEntity> listByWorkLine(Integer workLine);
}
