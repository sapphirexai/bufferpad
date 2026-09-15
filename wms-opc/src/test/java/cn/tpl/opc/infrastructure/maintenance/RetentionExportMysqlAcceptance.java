package cn.tpl.opc.infrastructure.maintenance;

import cn.tpl.opc.commons.scheme.request.ExportCushionsByTimeScheme;
import cn.tpl.opc.service.impl.CushionTimeExportService;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in integration acceptance runner. Never connects outside loopback or a dedicated fixture database. */
public class RetentionExportMysqlAcceptance {
    static ExportCushionsByTimeScheme range(String type,String start,String end) {
        var r=new ExportCushionsByTimeScheme();r.setTimeType(type);r.setStartTime(start);r.setEndTime(end);return r;
    }
    static void passed(String name) {System.out.println("ACCEPTANCE_PASS "+name);}
    public static void main(String[] args) throws Exception {
        int port=Integer.parseInt(args[0]);Path out=Path.of(args[1]);
        MysqlDataSource ds=new MysqlDataSource();ds.setURL("jdbc:mysql://127.0.0.1:"+port+"/retention_export_acceptance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");ds.setUser("root");
        var service=new ScanLogRetentionService(ds);var cutoff=LocalDateTime.of(2026,6,14,0,0);
        try (Connection c=ds.getConnection();Statement s=c.createStatement()) {
            try(ResultSet rs=s.executeQuery("SELECT DATABASE()")){rs.next();assertEquals("retention_export_acceptance",rs.getString(1));}
            assertEquals("SKIPPED_MISSING_INDEX",service.cleanup(cutoff,1000,300,0).outcome());passed("missing index skips without deleting");
            s.execute("ALTER TABLE scan_log ADD INDEX idx_scan_log_created_id(created_date,id), ALGORITHM=INPLACE, LOCK=NONE");
            s.execute("SELECT GET_LOCK(CONCAT('bp.scanLog.cleanup:',MD5(DATABASE())),0)");
            assertEquals("SKIPPED_LOCKED",service.cleanup(cutoff,1000,300,0).outcome());passed("database lock prevents concurrent cleanup");
            s.execute("DO RELEASE_LOCK(CONCAT('bp.scanLog.cleanup:',MD5(DATABASE())))");
            long started=System.nanoTime();
            var result=service.cleanup(cutoff,1000,300,0);
            assertEquals(1000000,result.deleted());assertEquals("COMPLETE",result.outcome());
            try(ResultSet rs=s.executeQuery("SELECT GROUP_CONCAT(id ORDER BY id) FROM scan_log")){rs.next();assertEquals("2,1000002,1000003,1000004",rs.getString(1));}
            passed("one million expired logs deleted in batches; exact cutoff and newer retained; ms="+(System.nanoTime()-started)/1000000);
            assertEquals(0,service.cleanup(cutoff,1000,300,0).deleted());passed("repeat cleanup idempotent");
            s.execute("INSERT INTO scan_log(id,created_date) VALUES(2000001,'2020-01-01'),(2000002,'2020-01-01')");
            var limited=service.cleanup(cutoff,1,1,1100);assertEquals(1,limited.deleted());assertEquals("TIME_BUDGET_REACHED",limited.outcome());
            assertEquals(1,service.cleanup(cutoff,1000,300,0).deleted());passed("time budget commits progress and next run resumes");
            s.execute("INSERT INTO scan_log(id,created_date) VALUES(2000003,'2020-01-01')");
            s.execute("CREATE TRIGGER acceptance_delete_failure BEFORE DELETE ON scan_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='acceptance failure'");
            assertThrows(IllegalStateException.class,()->service.cleanup(cutoff,1000,300,0));
            s.execute("DROP TRIGGER acceptance_delete_failure");
            assertEquals(1,service.cleanup(cutoff,1000,300,0).deleted());passed("SQL failure releases advisory lock and allows retry");
        }
        var export=new CushionTimeExportService(ds,100000);
        var last=range("LAST_USE","2030-01-02 00:00:00","2030-01-02 23:59:59");
        Path temporary;
        try(var file=export.generate(last)) {assertEquals(1007,file.rows());temporary=file.path();Files.copy(file.path(),out.resolve("last-use.xlsx"),StandardCopyOption.REPLACE_EXISTING);}
        assertFalse(Files.exists(temporary));passed("last use export crosses 1000-row batches and includes end-second fractions; temp deleted");
        try(var file=export.generate(range("FIRST_USE","2030-01-02 00:00:00","2030-01-02 23:59:59"))) {
            assertEquals(1,file.rows());Files.copy(file.path(),out.resolve("first-use.xlsx"),StandardCopyOption.REPLACE_EXISTING);
        }
        passed("first use selects a different result set and handles null last use");
        Set<Path> before;
        try(var paths=Files.list(Path.of(System.getProperty("java.io.tmpdir")))){before=paths.filter(p->p.getFileName().toString().startsWith("bufferpad-cushion-time-")).collect(java.util.stream.Collectors.toSet());}
        assertThrows(IllegalArgumentException.class,()->new CushionTimeExportService(ds,10).generate(last));
        assertThrows(IllegalArgumentException.class,()->export.generate(range("LAST_USE","2031-01-01 00:00:00","2031-01-01 00:00:00")));
        try(var paths=Files.list(Path.of(System.getProperty("java.io.tmpdir")))){assertEquals(before,paths.filter(p->p.getFileName().toString().startsWith("bufferpad-cushion-time-")).collect(java.util.stream.Collectors.toSet()));}
        passed("row cap and empty result return errors without leaked temporary files");
    }
}
