package cn.tpl.opc.infrastructure.maintenance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ScanLogCleanupJob {
    private final ScanLogRetentionService service;
    private final boolean enabled;
    private final int months, batchSize, maxSeconds, pauseMillis;
    private final Clock clock;
    private final ZoneId zone;
    private final AtomicBoolean running = new AtomicBoolean();

    @Autowired
    public ScanLogCleanupJob(ScanLogRetentionService service,
            @Value("${scan-log.retention.enabled:true}") boolean enabled,
            @Value("${scan-log.retention.months:3}") int months,
            @Value("${scan-log.retention.batch-size:1000}") int batchSize,
            @Value("${scan-log.retention.max-seconds:300}") int maxSeconds,
            @Value("${scan-log.retention.pause-millis:100}") int pauseMillis,
            @Value("${scan-log.retention.zone:Asia/Shanghai}") String zone) {
        this(service, enabled, months, batchSize, maxSeconds, pauseMillis, zone, Clock.systemUTC());
    }
    ScanLogCleanupJob(ScanLogRetentionService service, boolean enabled, int months, int batchSize,
                      int maxSeconds, int pauseMillis, String zone, Clock clock) {
        if (months < 1 || months > 120 || batchSize < 1 || batchSize > 10000 || maxSeconds < 1 || maxSeconds > 3600 || pauseMillis < 0 || pauseMillis > 10000)
            throw new IllegalArgumentException("Invalid scan-log.retention configuration");
        this.service=service; this.enabled=enabled; this.months=months; this.batchSize=batchSize;
        this.maxSeconds=maxSeconds; this.pauseMillis=pauseMillis; this.zone=ZoneId.of(zone); this.clock=clock;
    }
    @Scheduled(cron="${scan-log.retention.cleanup-cron:0 0 3 * * ?}", zone="${scan-log.retention.zone:Asia/Shanghai}")
    public void cleanupExpiredLogs() {
        if (!enabled || !running.compareAndSet(false, true)) return;
        long start=System.nanoTime();
        LocalDateTime cutoff=LocalDate.now(clock.withZone(zone)).minusMonths(months).atStartOfDay();
        try {
            ScanLogRetentionService.CleanupResult result=service.cleanup(cutoff,batchSize,maxSeconds,pauseMillis);
            log.info("scan_log cleanup completed: cutoff={}, zone={}, deleted={}, batches={}, outcome={}, elapsedMs={}",
                    cutoff,zone,result.deleted(),result.batches(),result.outcome(),(System.nanoTime()-start)/1000000);
        } catch (RuntimeException e) {
            log.error("scan_log cleanup task failed; next scheduled run may retry, cutoff={}",cutoff,e);
        } finally { running.set(false); }
    }
}
