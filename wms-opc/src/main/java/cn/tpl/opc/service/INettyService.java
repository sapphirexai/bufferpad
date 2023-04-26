package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty服务接口
 */
public interface INettyService {
    ResultDTO<Boolean> sendMsg(String ip, Integer port, String msg);

    /**
     * 连接所有设备，并返回当前所有设备状态信息
     *
     * @return 所有设备状态信息
     */
    ResultDTO<List<DeviceInfoDTO>> connectDevices();

    /**
     * 连接所有扫码器，并返回当前所有扫码器状态信息
     *
     * @return 所有扫码器状态信息
     */
    ResultDTO<List<DeviceInfoDTO>> connectScanners();

    /**
     * 获取当前所有扫码器状态信息
     *
     * @return 所有扫码器状态信息
     */
    List<DeviceInfoDTO> getScannersStatus();
}
