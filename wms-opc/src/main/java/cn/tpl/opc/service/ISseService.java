package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/21
 * Sse服务接口
 */
public interface ISseService {
    /**
     * 订阅设备状态
     *
     * @param clientId 客户端Id
     * @return SseEmitter 对象
     */
    SseEmitter subscribeDevicesStatus(String clientId);

    /**
     * 发送设备消息
     *
     * @param msg 设备消息
     */
    void sendDeviceMsg(SseMsgDTO<DeviceInfoDTO> msg);
}
