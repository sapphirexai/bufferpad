package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.util.FastJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SSE push service.
 */
@Slf4j
@Service("sseService")
public class SseServiceImpl implements ISseService {
    private static final ExecutorService MSG_SERVICE = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 4,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(Runtime.getRuntime().availableProcessors() * 4));

    private static final ConcurrentHashMap<String, SseEmitter> SSE_CLIENTS = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeDevicesStatus(String clientId) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.onCompletion(() -> {
            log.info("SseCompleted, clientId => {}", clientId);
            SSE_CLIENTS.remove(clientId, sseEmitter);
        });
        sseEmitter.onTimeout(() -> {
            log.info("SseTimeout, clientId => {}", clientId);
            removeSseClient(clientId, sseEmitter);
        });
        sseEmitter.onError((err) -> {
            log.error("SseError, clientId => {}", clientId, err);
            SSE_CLIENTS.remove(clientId, sseEmitter);
        });

        SseEmitter oldEmitter = SSE_CLIENTS.put(clientId, sseEmitter);
        if (oldEmitter != null) completeQuietly(oldEmitter);
        return sseEmitter;
    }

    @Override
    public <T> void sendMsg(ResultDTO<SseMsgDTO<T>> msg) {
        if (msg == null || msg.getData() == null || msg.getData().getWorkLine() == null) {
            log.warn("sendMsg skipped, invalid msg => {}", msg);
            return;
        }

        String fMsg;
        try {
            fMsg = FastJsonUtils.toJSONString(msg);
        } catch (Exception e) {
            log.error("sendMsg serialize failed, msg => {}", msg, e);
            return;
        }
        String workLine = String.valueOf(msg.getData().getWorkLine());
        for (Map.Entry<String, SseEmitter> entry : SSE_CLIENTS.entrySet()) {
            String clientId = entry.getKey();
            if (!clientId.equals(workLine)) continue;

            SseEmitter sseEmitter = entry.getValue();
            try {
                MSG_SERVICE.execute(() -> doSendMsg(clientId, sseEmitter, fMsg));
            } catch (RejectedExecutionException e) {
                log.error("sendMsg rejected, client => {}", clientId, e);
                removeSseClient(clientId, sseEmitter);
            }
        }
    }

    @Override
    public <T> void sendFailMsg(SseMsgDTO<T> msg, String reason) {
        sendMsg(ResultDTO.failure(msg, reason));
    }

    @Override
    public void sendDeviceMsg(DeviceInfoDTO deviceInfo) {
        sendMsg(ResultDTO.success(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_DEVICE_STATUS, deviceInfo, deviceInfo.getWorkLine(), deviceInfo.getInstallSeq())));
    }

    @Override
    public void sendCushionMsg(CushionInfoDTO cushionInfo) {
        String qrCode = cushionInfo.getQrCode();
        if (StringUtils.isEmpty(qrCode)) {
            sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, null, cushionInfo.getWorkLine(), cushionInfo.getScannerSeq()), Constants.SCANNER_MSG_NO_READ);
            return;
        }
        sendMsg(ResultDTO.success(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfo, cushionInfo.getWorkLine(), cushionInfo.getScannerSeq())));
    }

    private void doSendMsg(String clientId, SseEmitter sseEmitter, String fMsg) {
        try {
            log.info("sendMsg, msgJson => {}", fMsg);
            sseEmitter.send(fMsg);
        } catch (Exception e) {
            log.error("sendMsg, client => {}, exception", clientId, e);
            removeSseClient(clientId, sseEmitter);
        }
    }

    private void removeSseClient(String clientId, SseEmitter sseEmitter) {
        SSE_CLIENTS.remove(clientId, sseEmitter);
        completeQuietly(sseEmitter);
    }

    private void completeQuietly(SseEmitter sseEmitter) {
        try {
            sseEmitter.complete();
        } catch (Exception e) {
            log.debug("complete sse quietly failed", e);
        }
    }
}
