package cn.tpl.opc.infrastructure.maintenance;

import cn.tpl.opc.service.IOperationEventService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OperationEventCleanupJobTest {
    @Test
    public void cleanupDeletesEventsOlderThanThirtyDays() {
        IOperationEventService service = mock(IOperationEventService.class);
        Instant now = Instant.parse("2026-08-03T02:15:00Z");
        OperationEventCleanupJob job = new OperationEventCleanupJob(
                service, 30, Clock.fixed(now, ZoneOffset.UTC));

        job.cleanupExpiredEvents();

        ArgumentCaptor<Date> cutoffCaptor = ArgumentCaptor.forClass(Date.class);
        verify(service).deleteBefore(cutoffCaptor.capture());
        Assert.assertEquals(Date.from(Instant.parse("2026-07-04T02:15:00Z")), cutoffCaptor.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void retentionDaysMustBePositive() {
        new OperationEventCleanupJob(
                mock(IOperationEventService.class), 0, Clock.systemUTC());
    }
}
