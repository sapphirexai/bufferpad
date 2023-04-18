package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.netty.MsgBus;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.INettyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

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


    @Override
    public ResultDTO<Boolean> sendMsg(String ip, Integer port, String msg) {
        nettyMsgBus.sendMsg(ip, port, msg);
        return ResultDTO.success(true);
    }

    @Override
    public ResultDTO<Boolean> connectScanner() {
        List<DeviceInfoEntity> deviceInfoEntities = deviceService.listDeviceInfoByType(0);
        if (CollectionUtils.isEmpty(deviceInfoEntities))
            return ResultDTO.failure("无扫码器");
        for (DeviceInfoEntity deviceInfo : deviceInfoEntities) {
            sendMsg(deviceInfo.getIp(), deviceInfo.getPort(), "connectScanner");
        }
        return ResultDTO.success(true);
    }
}
