package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.util.FastJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
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
    private final ExecutorService msgService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 4,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(Runtime.getRuntime().availableProcessors() * 4));

    private final ConcurrentHashMap<String, SseSession> sseClients = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeDevicesStatus(String clientId) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        String sessionId = clientId + "-" + UUID.randomUUID();
        sseEmitter.onCompletion(() -> {
            log.info("SseCompleted, sessionId => {}", sessionId);
            sseClients.remove(sessionId);
        });
        sseEmitter.onTimeout(() -> {
            log.info("SseTimeout, sessionId => {}", sessionId);
            removeSseClient(sessionId, sseEmitter);
        });
        sseEmitter.onError((err) -> {
            log.warn("SseError, sessionId => {}, reason => {}", sessionId, err.getMessage());
            sseClients.remove(sessionId);
        });

        sseClients.put(sessionId, new SseSession(clientId, sseEmitter));
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
        for (Map.Entry<String, SseSession> entry : sseClients.entrySet()) {
            String sessionId = entry.getKey();
            SseSession session = entry.getValue();
            if (!session.workLine.equals(workLine)) continue;

            SseEmitter sseEmitter = session.emitter;
            try {
                msgService.execute(() -> doSendMsg(sessionId, sseEmitter, fMsg));
            } catch (RejectedExecutionException e) {
                log.error("sendMsg rejected, session => {}", sessionId, e);
                removeSseClient(sessionId, sseEmitter);
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

    @Override
    public void sendOperationEvent(OperationEventDTO event) {
        sendMsg(ResultDTO.success(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_OPERATION_EVENT, event, event.getWorkLine(), event.getScannerSeq())));
    }

    private void doSendMsg(String sessionId, SseEmitter sseEmitter, String fMsg) {
        try {
            log.info("sendMsg, msgJson => {}", fMsg);
            sseEmitter.send(fMsg);
        } catch (Exception e) {
            log.debug("sendMsg failed, session => {}, reason => {}", sessionId, e.getMessage());
            removeSseClient(sessionId, sseEmitter);
        }
    }

    private void removeSseClient(String sessionId, SseEmitter sseEmitter) {
        SseSession removed = sseClients.remove(sessionId);
        if (removed == null || removed.emitter != sseEmitter) return;
        completeQuietly(sseEmitter);
    }

    private void completeQuietly(SseEmitter sseEmitter) {
        try {
            sseEmitter.complete();
        } catch (Exception e) {
            log.debug("complete sse quietly failed", e);
        }
    }

    @PreDestroy
    public void destroy() {
        for (SseSession session : sseClients.values()) completeQuietly(session.emitter);
        sseClients.clear();
        msgService.shutdownNow();
    }

    private static final class SseSession {
        private final String workLine;
        private final SseEmitter emitter;

        private SseSession(String workLine, SseEmitter emitter) {
            this.workLine = workLine;
            this.emitter = emitter;
        }
    }
}
