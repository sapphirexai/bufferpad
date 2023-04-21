package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.service.ISseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    public boolean subscribeDevicesStatus(String clientId) {
        SSE_CLIENTS.remove(clientId);
        SSE_CLIENTS.put(clientId, new SseEmitter(0L));
        return true;
    }

    @Override
    public void sendDeviceMsg(SseMsgDTO<DeviceInfoDTO> msg) {
        for (SseEmitter sseEmitter : SSE_CLIENTS.values()) {
            MSG_SERVICE.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        sseEmitter.send(ResultDTO.success(msg));
                    } catch (Exception e) {
                        log.error("sendDeviceMsg异常：", e);

                    }
                }
            });
        }
    }
}
