package cn.tpl.opc.infrastructure.maintenance;

import org.junit.Test;
import java.time.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ScanLogCleanupJobTest {
    private ScanLogCleanupJob job(ScanLogRetentionService s, boolean enabled, String instant) {
        return new ScanLogCleanupJob(s,enabled,3,1000,300,100,"Asia/Shanghai",Clock.fixed(Instant.parse(instant),ZoneOffset.UTC));
    }
    @Test public void usesBeijingDateAndCalendarMonths() {
        ScanLogRetentionService s=mock(ScanLogRetentionService.class);
        when(s.cleanup(any(),anyInt(),anyInt(),anyInt())).thenReturn(new ScanLogRetentionService.CleanupResult(0,1,"COMPLETE"));
        job(s,true,"2026-09-13T16:00:00Z").cleanupExpiredLogs();
        verify(s).cleanup(LocalDateTime.of(2026,6,14,0,0),1000,300,100);
        job(s,true,"2024-05-30T19:00:00Z").cleanupExpiredLogs();
        verify(s).cleanup(LocalDateTime.of(2024,2,29,0,0),1000,300,100);
    }
    @Test public void disabledDoesNotAccessDatabase() {
        ScanLogRetentionService s=mock(ScanLogRetentionService.class);
        job(s,false,"2026-09-14T00:00:00Z").cleanupExpiredLogs();verifyNoInteractions(s);
    }
    @Test public void failureAllowsNextRun() {
        ScanLogRetentionService s=mock(ScanLogRetentionService.class);
        when(s.cleanup(any(),anyInt(),anyInt(),anyInt())).thenThrow(new IllegalStateException("test failure"))
            .thenReturn(new ScanLogRetentionService.CleanupResult(2,1,"COMPLETE"));
        ScanLogCleanupJob j=job(s,true,"2026-09-14T00:00:00Z");
        assertDoesNotThrow(j::cleanupExpiredLogs);j.cleanupExpiredLogs();
        verify(s,times(2)).cleanup(any(),anyInt(),anyInt(),anyInt());
    }
    @Test public void overlappingRunsAreSkipped() throws Exception {
        ScanLogRetentionService s=mock(ScanLogRetentionService.class);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        when(s.cleanup(any(),anyInt(),anyInt(),anyInt())).thenAnswer(a->{entered.countDown();release.await(5,TimeUnit.SECONDS);return new ScanLogRetentionService.CleanupResult(0,1,"COMPLETE");});
        ScanLogCleanupJob j=job(s,true,"2026-09-14T00:00:00Z");
        Thread first=new Thread(j::cleanupExpiredLogs);first.start();
        try {assertTrue(entered.await(5,TimeUnit.SECONDS));j.cleanupExpiredLogs();verify(s,times(1)).cleanup(any(),anyInt(),anyInt(),anyInt());}
        finally {release.countDown();first.join(5000);}
    }
}
