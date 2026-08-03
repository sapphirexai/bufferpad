package cn.tpl.opc.infrastructure.maintenance;

import cn.tpl.opc.service.IOperationEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class OperationEventCleanupJob {
    private final IOperationEventService operationEventService;
    private final int retentionDays;
    private final Clock clock;

    @Autowired
    public OperationEventCleanupJob(
            IOperationEventService operationEventService,
            @Value("${operation-event.retention.days:30}") int retentionDays) {
        this(operationEventService, retentionDays, Clock.systemUTC());
    }

    OperationEventCleanupJob(IOperationEventService operationEventService, int retentionDays, Clock clock) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("operation-event.retention.days must be greater than 0");
        }
        this.operationEventService = operationEventService;
        this.retentionDays = retentionDays;
        this.clock = clock;
    }

    @Scheduled(cron = "${operation-event.retention.cleanup-cron:0 15 2 * * ?}")
    public void cleanupExpiredEvents() {
        Instant cutoffInstant = clock.instant().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = operationEventService.deleteBefore(Date.from(cutoffInstant));
        log.info("operation event cleanup completed, retentionDays => {}, deleted => {}",
                retentionDays, deleted);
    }
}
