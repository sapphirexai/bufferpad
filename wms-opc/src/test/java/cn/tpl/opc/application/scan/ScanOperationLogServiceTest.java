package cn.tpl.opc.application.scan;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.util.*;
import static org.junit.Assert.*;

public class ScanOperationLogServiceTest {
    private JdbcTemplate db;
    private ScanOperationLogService logs;
    private DataSourceTransactionManager manager;
    @Before public void setup() {
        JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:summary"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        db=new JdbcTemplate(ds);manager=new DataSourceTransactionManager(ds);logs=new ScanOperationLogService(db,manager);
        db.execute("CREATE TABLE scan_log(id BIGINT AUTO_INCREMENT PRIMARY KEY,operation_id VARCHAR(64) UNIQUE,qr_code VARCHAR(255),msg TEXT,msg_type SMALLINT,created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,operation_type VARCHAR(24),status VARCHAR(16),result_code VARCHAR(48),work_line INT,operator_name VARCHAR(64),scanner_id BIGINT,plc_id BIGINT,scanner_snapshot VARCHAR(1024),plc_snapshot VARCHAR(1024),detail_json TEXT,updated_date TIMESTAMP)");
        db.execute("CREATE TABLE device_info(id BIGINT PRIMARY KEY,name VARCHAR(64),ip VARCHAR(64),port INT,position VARCHAR(64),install_seq INT,work_line INT)");
        db.execute("CREATE TABLE device_install_position(id INT PRIMARY KEY,name VARCHAR(64))");
        db.update("INSERT INTO device_install_position VALUES(1,'上'),(5,'位置5')");
        db.update("INSERT INTO device_info VALUES(1,'Scanner','192.0.2.22',15000,'',1,1),(3,'PLC_Up','192.0.2.23',502,'',1,1),(10,'PLC_Up','192.0.2.23',502,'',5,1)");
    }
    private void begin(String id){assertTrue(logs.begin(new ScanCommand(id,1L,1,"192.0.2.22","Scanner","上",1,"QR")));}
    private OperationEventDTO event(String id,OperationEventCode code){var e=OperationEventDTO.of(code,1);e.setOperationId(id);e.setScannerId(1L);e.setPlcId(3L);e.setQrCode("QR");return e;}
    private Map<String,Object> row(String id){return db.queryForMap("SELECT * FROM scan_log WHERE operation_id=?",id);}
    private void assertSeparateIdentities(String id) {
        Map<String,Object> record=row(id);
        assertEquals("QR",record.get("qr_code"));
        assertEquals(1L,((Number)record.get("scanner_id")).longValue());
        assertEquals(3L,((Number)record.get("plc_id")).longValue());
        assertTrue(record.get("scanner_snapshot").toString().contains("Scanner[ID=1"));
        assertTrue(record.get("scanner_snapshot").toString().contains("192.0.2.22:15000"));
        assertTrue(record.get("plc_snapshot").toString().contains("PLC_Up[ID=3"));
        assertTrue(record.get("plc_snapshot").toString().contains("192.0.2.23:502"));
        String msg=record.get("msg").toString();
        for(String identity:List.of("二维码=","扫码器=","PLC=","QR","Scanner","PLC_Up","192.168.20.","ID=3"))
            assertFalse("Summary repeats identity: "+identity,msg.contains(identity));
    }
    @Test public void oneRowIncludesDeviceSnapshotsAndWaitsForRead() {
        begin("a");var scan=event("a",OperationEventCode.SCAN_COUNTED);scan.setUsedCount(20);scan.setMaxUseCount(100);logs.accept(scan);
        var plc=event("a",OperationEventCode.PLC_NOTIFY_SUCCEEDED);plc.setReadExpected(true);plc.setAddress("D100");plc.setWriteValue((short)1);logs.accept(plc);
        assertEquals("PROCESSING",row("a").get("status"));logs.accept(event("a",OperationEventCode.PLC_READ_SUCCEEDED));
        assertEquals("SUCCESS",row("a").get("status"));String msg=(String)row("a").get("msg");
        assertTrue(msg.contains("当前次数=20"));assertTrue(msg.contains("寿命上限=100"));assertTrue(msg.contains("D100"));
        assertSeparateIdentities("a");
        assertFalse(logs.begin(new ScanCommand("a",1L,1,"","","",1,"QR")));
        logs.accept(plc);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM scan_log",Integer.class).intValue());
    }
    @Test public void manualScanKeepsFailureDiagnosticsWithoutRepeatingIdentities() {
        assertTrue(logs.begin(new ScanCommand("manual",null,1,"","","",null,"QR")));
        logs.accept(event("manual",OperationEventCode.SCAN_REPEATED));
        var failure=event("manual",OperationEventCode.PLC_WRITE_FAILED);
        failure.setAddress("D100");failure.setTechnicalDetail("连接已断开");logs.accept(failure);
        assertSeparateIdentities("manual");
        String msg=row("manual").get("msg").toString();
        assertTrue(msg.contains("手动扫码"));assertTrue(msg.contains("操作用户="));
        assertTrue(msg.contains("本次未增加使用次数"));assertTrue(msg.contains("原因=连接已断开"));
        assertTrue(msg.contains("地址=D100"));assertEquals("FAILED",row("manual").get("status"));
    }
    @Test public void standalonePlcReadKeepsResultAndDeviceColumns() {
        var read=event("standalone",OperationEventCode.PLC_READ_SUCCEEDED);
        read.setAddress("D200");read.setMessage("回读成功，开口数=42，已保存");logs.accept(read);
        assertSeparateIdentities("standalone");
        String msg=row("standalone").get("msg").toString();
        assertTrue(msg.contains("独立PLC操作"));assertTrue(msg.contains("开口数=42"));assertTrue(msg.contains("地址=D200"));
        assertEquals("SUCCESS",row("standalone").get("status"));
    }
    @Test public void outOfOrderAndLatePendingCannotHideReadFailure() {
        begin("b");logs.accept(event("b",OperationEventCode.PLC_READ_FAILED));
        var plc=event("b",OperationEventCode.PLC_NOTIFY_SUCCEEDED);plc.setReadExpected(true);logs.accept(plc);
        logs.accept(event("b",OperationEventCode.SCAN_COUNTED));
        var pending=event("b",OperationEventCode.PLC_NOTIFY_PENDING);pending.setReadExpected(true);logs.accept(pending);
        assertEquals("WARNING",row("b").get("status"));assertTrue(((String)row("b").get("msg")).contains("读取失败"));
    }
    @Test public void plcFailureRemainsFinalDespiteLatePlan() {
        begin("c");logs.accept(event("c",OperationEventCode.SCAN_COUNTED));logs.accept(event("c",OperationEventCode.PLC_WRITE_FAILED));
        var pending=event("c",OperationEventCode.PLC_NOTIFY_PENDING);pending.setReadExpected(true);logs.accept(pending);
        assertEquals("FAILED",row("c").get("status"));
    }
    @Test public void snapshotSurvivesRenameAndDistinctIdsRemainDistinct() {
        begin("d");logs.accept(event("d",OperationEventCode.SCAN_COUNTED));
        db.update("UPDATE device_info SET name='Changed' WHERE id=3");logs.accept(event("d",OperationEventCode.PLC_NOTIFY_SUCCEEDED));
        assertTrue(row("d").get("plc_snapshot").toString().contains("PLC_Up"));
        begin("e");var e=event("e",OperationEventCode.PLC_OFFLINE);e.setPlcId(10L);logs.accept(e);
        assertTrue(row("e").get("plc_snapshot").toString().contains("位置5"));assertTrue(row("e").get("plc_snapshot").toString().contains("ID=10"));
    }
    @Test public void timeoutMarksUnknownAndLateResultsFinishSameRow() {
        begin("f");db.update("UPDATE scan_log SET updated_date=?",new Date(0));assertEquals(1,logs.expirePending(new Date(1000)));
        assertEquals("UNKNOWN",row("f").get("status"));assertEquals(0,logs.expirePending(new Date()));
        logs.accept(event("f",OperationEventCode.SCAN_REPEATED));logs.accept(event("f",OperationEventCode.PLC_NOTIFY_SUCCEEDED));
        assertEquals("WARNING",row("f").get("status"));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM scan_log",Integer.class).intValue());
    }
    @Test public void databaseFailureAndNoReadAreExplicit() {
        begin("g");logs.accept(event("g",OperationEventCode.SCAN_COUNT_FAILED));assertEquals("FAILED",row("g").get("status"));
        begin("h");logs.accept(event("h",OperationEventCode.SCAN_NO_READ));logs.accept(event("h",OperationEventCode.PLC_NOTIFY_SUCCEEDED));assertEquals("FAILED",row("h").get("status"));
    }
    @Test public void transactionRollbackFinalizesFailedWithoutLosingTheLog() {
        begin("rollback");
        new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(tx->{
            logs.trackTransaction("rollback");db.update("UPDATE device_info SET name='should rollback' WHERE id=1");tx.setRollbackOnly();
        });
        assertEquals("FAILED",row("rollback").get("status"));assertEquals("SCAN_COUNT_FAILED",row("rollback").get("result_code"));
        assertEquals("Scanner",db.queryForObject("SELECT name FROM device_info WHERE id=1",String.class));
        assertTrue(row("rollback").get("msg").toString().contains("PLC指令未提交"));
    }
}
