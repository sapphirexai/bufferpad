package cn.tpl.opc.application.scan;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Explicit opt-in acceptance against an isolated fixture database and the production SQL firewall. */
public class ScanSummaryMysqlAcceptance {
    static OperationEventDTO event(String id,OperationEventCode code){var e=OperationEventDTO.of(code,1);e.setOperationId(id);e.setScannerId(1L);e.setPlcId(3L);e.setQrCode("SUMMARY-JAVA");return e;}
    static void pass(String text){System.out.println("ACCEPTANCE_PASS "+text);}
    public static void main(String[] args) throws Exception {
        try(DruidDataSource ds=new DruidDataSource()) {
            ds.setUrl("jdbc:mysql://127.0.0.1:"+Integer.parseInt(args[0])+"/scan_summary_acceptance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");ds.setUsername("root");ds.setFilters("wall");ds.setMaxActive(12);
            JdbcTemplate db=new JdbcTemplate(ds);var manager=new DataSourceTransactionManager(ds);var service=new ScanOperationLogService(db,manager);
            assertEquals("scan_summary_acceptance",db.queryForObject("SELECT DATABASE()",String.class));
            assertTrue(service.begin(new ScanCommand("parallel-op",1L,1,"","","",1,"SUMMARY-JAVA")));
            ExecutorService executor=Executors.newFixedThreadPool(8);
            try {
                List<Callable<Void>> calls=new ArrayList<>();
                for(int i=0;i<120;i++){final int phase=i%3;calls.add(()->{var e=event("parallel-op",phase==0?OperationEventCode.SCAN_COUNTED:phase==1?OperationEventCode.PLC_NOTIFY_SUCCEEDED:OperationEventCode.PLC_READ_SUCCEEDED);e.setReadExpected(true);service.accept(e);return null;});}
                for(Future<Void> future:executor.invokeAll(calls))future.get();
            } finally {executor.shutdownNow();}
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM scan_log WHERE operation_id='parallel-op'",Integer.class).intValue());
            assertEquals("SUCCESS",db.queryForObject("SELECT status FROM scan_log WHERE operation_id='parallel-op'",String.class));
            var pending=event("parallel-op",OperationEventCode.PLC_NOTIFY_PENDING);pending.setReadExpected(true);service.accept(pending);
            assertEquals("SUCCESS",db.queryForObject("SELECT status FROM scan_log WHERE operation_id='parallel-op'",String.class));
            pass("120 concurrent and repeated callbacks converge to one final row; late plan does not regress status");
            assertFalse(service.begin(new ScanCommand("parallel-op",1L,1,"","","",1,"SUMMARY-JAVA")));pass("duplicate operation id rejected by unique key");
            service.begin(new ScanCommand("rollback-op",1L,1,"","","",1,"SUMMARY-ROLLBACK"));
            TransactionTemplate tx=new TransactionTemplate(manager);
            try {tx.executeWithoutResult(status->{db.update("INSERT INTO cushion_info(qr_code) VALUES('SUMMARY-ROLLBACK')");throw new IllegalStateException("rollback fixture");});}catch(IllegalStateException expected){service.uncertain("rollback-op","业务事务异常，结果需核实");}
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM cushion_info WHERE qr_code='SUMMARY-ROLLBACK'",Integer.class).intValue());
            assertEquals("UNKNOWN",db.queryForObject("SELECT status FROM scan_log WHERE operation_id='rollback-op'",String.class));pass("independent operation record survives business rollback");
            service.begin(new ScanCommand("timeout-op",1L,1,"","","",1,"SUMMARY-TIMEOUT"));
            db.update("UPDATE scan_log SET updated_date='2020-01-01' WHERE operation_id='timeout-op'");assertEquals(1,service.expirePending(new Date(System.currentTimeMillis()-300000)));
            assertEquals("UNKNOWN",db.queryForObject("SELECT status FROM scan_log WHERE operation_id='timeout-op'",String.class));
            service.accept(event("timeout-op",OperationEventCode.SCAN_COUNTED));service.accept(event("timeout-op",OperationEventCode.PLC_NOTIFY_SUCCEEDED));
            assertEquals("SUCCESS",db.queryForObject("SELECT status FROM scan_log WHERE operation_id='timeout-op'",String.class));pass("restart-style stale operation is reconciled and late results update same row");
            service.begin(new ScanCommand("long-op",1L,1,"","","",1,"SUMMARY-LONG"));var longEvent=event("long-op",OperationEventCode.SCAN_COUNTED);longEvent.setMessage("详细结果".repeat(400));service.accept(longEvent);service.accept(event("long-op",OperationEventCode.PLC_OFFLINE));
            String msg=db.queryForObject("SELECT msg FROM scan_log WHERE operation_id='long-op'",String.class);assertTrue(msg.length()>255);assertTrue(msg.length()<=8000);pass("TEXT stores complete long summary without old 255-character failure");
            db.update("INSERT INTO scan_log(qr_code,created_date) VALUES('SUMMARY-CLEANUP','2010-01-01')");
            var cleanup=new cn.tpl.opc.infrastructure.maintenance.ScanLogRetentionService(ds).cleanup(java.time.LocalDateTime.of(2011,1,1,0,0),1000,10,0);
            assertEquals(1,cleanup.deleted());assertEquals("COMPLETE",cleanup.outcome());pass("retention cleanup and legacy default msg remain compatible with new schema");
        }
    }
}
