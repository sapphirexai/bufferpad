package cn.tpl.opc.netty;

import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import cn.tpl.opc.infrastructure.plc.PlcErrorClassifier;
import cn.tpl.opc.infrastructure.plc.PlcFailureType;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.*;

/** Configured Int16 reads only; never updates cushions, writes registers or emits scan logs. */
@Component
public class PlcHealthMonitor {
    @Resource private ConnectionMgr connectionMgr;
    @Resource private DeviceHealthProperties properties;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Long> nextRun = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ExecutorService workers;
    @PostConstruct public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16), new ThreadPoolExecutor.AbortPolicy());
        scheduler.scheduleWithFixedDelay(this::tick, 1, 1, TimeUnit.SECONDS);
    }
    private void tick() {
        for (Connection connection : connectionMgr.getConnections().values()) {
            DeviceHealthProperties.PlcProbe config = properties.getPlcProbes().get(connection.getId());
            if (config == null || !config.isEnabled() || config.getAddress() == null || config.getAddress().isBlank()
                    || connection.getType() == null || connection.getType() == 0 || !connection.isActive()) continue;
            long now = System.nanoTime();
            if (now < nextRun.getOrDefault(connection.getId(), 0L) || !inFlight.add(connection.getId())) continue;
            try {
                workers.execute(() -> {
                    try { probe(connection, config, properties.getFailureThreshold()); }
                    finally {
                        nextRun.put(connection.getId(), System.nanoTime() + TimeUnit.SECONDS.toNanos(config.getIntervalSeconds()));
                        inFlight.remove(connection.getId());
                    }
                });
            } catch (RejectedExecutionException full) { inFlight.remove(connection.getId()); }
        }
        nextRun.keySet().removeIf(id -> !connectionMgr.connectionExists(id));
    }
    public void probe(Connection connection, DeviceHealthProperties.PlcProbe config, int threshold) {
        if (!config.isEnabled() || config.getAddress() == null || config.getAddress().isBlank()) return;
        synchronized (connection.getPlcIoLock()) {
            if (!connection.isActive() || connection.isNoPLCNet()) return;
            Object client = connection.getPlcClient();
            connection.setMonitoringMode("READ_PROBE");
            connection.recordRequest("PLC_READ_PROBE");
            PlcIoResult<Short> result = connection.readInt16(config.getAddress());
            synchronized (connection) {
                if (connection.getPlcClient() != client) return;
                if (result.isSuccess()) connection.markMonitoringSuccess();
                else applyFailure(connection, result, threshold);
            }
        }
    }
    public static boolean isTimeout(PlcIoResult<?> result) {
        String message = result.getMessage() == null ? "" : result.getMessage().toLowerCase(java.util.Locale.ROOT);
        return message.contains("timeout") || message.contains("timed out") || message.contains("超时");
    }
    public static void applyFailure(Connection connection, PlcIoResult<?> result, int threshold) {
        if (isTimeout(result)) connection.recordRequestTimeout(threshold, result.getErrorCode());
        else if (new PlcErrorClassifier().classify(result.getErrorCode(), result.getMessage()) == PlcFailureType.TRANSPORT)
            connection.nowDead("PLC网络或连接异常", result.getErrorCode());
        else connection.markProbeRejected(result.getErrorCode());
    }
    @PreDestroy public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
        if (workers != null) workers.shutdownNow();
    }
}
