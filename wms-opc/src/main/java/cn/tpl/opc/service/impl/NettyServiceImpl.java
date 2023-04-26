package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.netty.Connector;
import cn.tpl.opc.netty.MsgBus;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.INettyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty服务
 */
@Slf4j
@Service("nettyService")
public class NettyServiceImpl implements INettyService {
    @Resource
    private MsgBus nettyMsgBus;
    @Resource
    private IDeviceInfoService deviceService;
    @Resource
    private Connector connector;
    @Resource
    private ConnectionMgr connectionMgr;

    @Override
    public ResultDTO<Boolean> sendMsg(String ip, Integer port, String msg) {
        nettyMsgBus.sendMsg(ip, port, msg);
        return ResultDTO.success(true);
    }

    @Override
    public ResultDTO<List<DeviceInfoDTO>> connectDevices() {
        List<DeviceInfoEntity> deviceInfoEntities = deviceService.list();
        if (CollectionUtils.isEmpty(deviceInfoEntities))
            return ResultDTO.failure("无设备");
        return ResultDTO.success(deviceInfoEntities.stream().map(this::device2DTO).collect(Collectors.toList()));
    }

    @Override
    public ResultDTO<List<DeviceInfoDTO>> connectScanners() {
        List<DeviceInfoEntity> deviceInfoEntities = deviceService.listDeviceInfoByType(0);
        if (CollectionUtils.isEmpty(deviceInfoEntities))
            return ResultDTO.failure("无扫码器");

        return ResultDTO.success(deviceInfoEntities.stream().map(this::device2DTO).collect(Collectors.toList()));
    }

    @Override
    public List<DeviceInfoDTO> getScannersStatus() {
        return connectionMgr.getConnections().values().stream().map(this::connection2DeviceDTO).collect(Collectors.toList());
    }

    private DeviceInfoDTO device2DTO(DeviceInfoEntity deviceInfo) {
        DeviceInfoDTO deviceInfoDTO = new DeviceInfoDTO();
        Connection connection = new Connection();
        BeanUtils.copyProperties(deviceInfo, connection);
        BeanUtils.copyProperties(deviceInfo, deviceInfoDTO);
        boolean isActive = connector.connect(connection);
        if (isActive)
            deviceInfoDTO.setStatus(Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE);
        return deviceInfoDTO;
    }

    private DeviceInfoDTO connection2DeviceDTO(Connection connection) {
        DeviceInfoDTO deviceInfoDTO = new DeviceInfoDTO();
        BeanUtils.copyProperties(connection, deviceInfoDTO);
        return deviceInfoDTO;
    }
}
