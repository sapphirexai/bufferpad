package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
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
     * 发送消息
     *
     * @param msg 消息
     */
    <T> void sendMsg(ResultDTO<SseMsgDTO<T>> msg);

    <T> void sendFailMsg(SseMsgDTO<T> msg, String reason);

    /**
     * 发送设备消息
     *
     * @param deviceInfo 设备数据
     */
    void sendDeviceMsg(DeviceInfoDTO deviceInfo);

    /**
     * 发送缓冲垫消息
     *
     * @param cushionInfo 缓冲垫数据
     */
    void sendCushionMsg(CushionInfoDTO cushionInfo);

    void sendOperationEvent(OperationEventDTO event);
}
