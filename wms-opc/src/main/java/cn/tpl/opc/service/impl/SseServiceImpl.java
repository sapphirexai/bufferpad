package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.util.FastJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/21
 * Sse服务
 */
@Slf4j
@Service("sseService")
public class SseServiceImpl implements ISseService {
    /**
     * 消息发送线程池
     */
    private static final ExecutorService MSG_SERVICE = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 4,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(Runtime.getRuntime().availableProcessors() * 4));
    /**
     * 订阅Sse消息的客户端，key为clientId
     */
    private static final ConcurrentHashMap<String, SseEmitter> SSE_CLIENTS = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeDevicesStatus(String clientId) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.onError((err) -> log.error("SseError，clientId：{}，异常：{}", err.getMessage(), clientId));
        SSE_CLIENTS.remove(clientId);
        SSE_CLIENTS.put(clientId, sseEmitter);
        return sseEmitter;
    }

    @Override
    public <T> void sendMsg(SseMsgDTO<T> msg) {
        for (Map.Entry<String, SseEmitter> entry : SSE_CLIENTS.entrySet()) {
            MSG_SERVICE.execute(() -> {
                try {
                    String fMsg = FastJsonUtils.toJSONString(ResultDTO.success(msg));
                    log.info("sendMsg，msgJson：{}", fMsg);
                    entry.getValue().send(fMsg);
                } catch (Exception e) {
                    log.error("sendMsg，Client：{}，异常", entry.getKey(), e);
                    SSE_CLIENTS.remove(entry.getKey());
                }
            });
        }
    }

    @Override
    public void sendDeviceMsg(DeviceInfoDTO deviceInfo) {
        sendMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_DEVICE_STATUS, deviceInfo));
    }

    @Override
    public void sendCushionMsg(CushionInfoDTO cushionInfo) {
        if (null == cushionInfo) {
            sendMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, Constants.SCANNER_MSG_NO_READ));
            return;
        }
        sendMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfo));
    }
}
