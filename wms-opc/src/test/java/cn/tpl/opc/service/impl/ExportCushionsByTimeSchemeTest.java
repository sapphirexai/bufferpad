package cn.tpl.opc.service.impl;
import cn.tpl.opc.commons.scheme.request.ExportCushionsByTimeScheme;
import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
public class ExportCushionsByTimeSchemeTest {
    private ExportCushionsByTimeScheme request(String start,String end) {
        ExportCushionsByTimeScheme r=new ExportCushionsByTimeScheme();r.setStartTime(start);r.setEndTime(end);return r;
    }
    @Test public void defaultLastAndExplicitFirstHaveSafeColumns() {
        var r=request("2026-09-14 00:00:00","2026-09-14 23:59:59");
        assertEquals("last_scan_date",r.validatedRange().column());
        r.setTimeType("FIRST_USE");assertEquals("created_date",r.validatedRange().column());
        r.setTimeType("created_date OR 1=1");assertThrows(IllegalArgumentException.class,r::validatedRange);
        r.setTimeType(null);assertThrows(IllegalArgumentException.class,r::validatedRange);
    }
    @Test public void endIncludesItsWholeSecondAndRollsToNextDay() {
        var range=request("2024-02-29 23:59:59","2024-02-29 23:59:59").validatedRange();
        assertEquals(LocalDateTime.of(2024,3,1,0,0),range.endExclusive());
    }
    @Test public void rejectsInvalidMissingAndReverseDates() {
        for (String invalid:new String[]{null,"","2026-02-29 00:00:00","2026-09-14","2026-09-14 25:00:00"})
            assertThrows(IllegalArgumentException.class,()->request(invalid,"2026-09-14 23:59:59").validatedRange());
        assertThrows(IllegalArgumentException.class,()->request("2026-09-15 00:00:00","2026-09-14 00:00:00").validatedRange());
    }
}
