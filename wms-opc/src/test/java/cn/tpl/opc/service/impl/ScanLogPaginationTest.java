package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.dto.result.ScanLogDTO;
import cn.tpl.opc.commons.scheme.request.QueryScanLogScheme;
import cn.tpl.opc.config.MybatisPlusConfig;
import cn.tpl.opc.mapper.ScanLogEntityMapper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Timestamp;
import java.util.UUID;
import static org.junit.Assert.*;

/** Exercises the real mapper and pagination interceptor, without starting device connections. */
public class ScanLogPaginationTest {
    private SqlSession session;
    private ScanLogServiceImpl service;
    private final Timestamp early = Timestamp.valueOf("2024-01-02 03:04:05.123");
    private final Timestamp late = Timestamp.valueOf("2024-01-03 03:04:05.123");
    private JdbcTemplate fixture;

    @Before public void setup() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:logs_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        fixture=jdbc;
        jdbc.execute("CREATE TABLE scan_log(id BIGINT PRIMARY KEY,qr_code VARCHAR(255),msg VARCHAR(255),created_date TIMESTAMP(3),msg_type SMALLINT,operation_id varchar(64),operation_type varchar(24),status varchar(16),result_code varchar(48),work_line int,operator_name varchar(64),scanner_id bigint,plc_id bigint,scanner_snapshot varchar(1024),plc_snapshot varchar(1024),detail_json text,updated_date datetime(3))");
        for (int id = 1; id <= 65; id++) {
            jdbc.update("INSERT INTO scan_log(id,qr_code,msg,created_date,msg_type) VALUES(?,?,?,?,?)", id, id % 2 == 0 ? "match" : "other", "row-" + id, id <= 40 ? early : late, id % 2);
        }
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new ClassPathResource("mapper/ScanLogEntityMapper.xml"));
        factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor());
        session = factory.getObject().openSession(true);
        service = new ScanLogServiceImpl();
        ReflectionTestUtils.setField(service, "scanLogEntityMapper", session.getMapper(ScanLogEntityMapper.class));
    }

    @After public void close() { if (session != null) session.close(); }

    @Test public void defaultAndSecondPageHaveStableOrderAndCorrectTotal() {
        QueryScanLogScheme query = new QueryScanLogScheme();
        PageData<ScanLogDTO> first = service.listByPage(query);
        assertEquals(20, first.getPageSize());
        assertEquals(65, first.getTotalPage());
        assertEquals(20, first.getData().size());
        assertEquals(Long.valueOf(65), first.getData().get(0).getId());
        assertEquals(Long.valueOf(46), first.getData().get(19).getId());
        query.setCurrentPage(2);
        PageData<ScanLogDTO> second = service.listByPage(query);
        assertEquals(2, second.getCurrentPage());
        assertEquals(Long.valueOf(45), second.getData().get(0).getId());
        assertEquals(Long.valueOf(26), second.getData().get(19).getId());
    }

    @Test public void invalidSizesCannotDisablePagination() {
        for (int size : new int[]{-1, 0, Integer.MAX_VALUE}) {
            QueryScanLogScheme query = new QueryScanLogScheme();
            query.setPageSize(size); query.setCurrentPage(-1);
            PageData<ScanLogDTO> page = service.listByPage(query);
            assertEquals(1, page.getCurrentPage());
            assertEquals(size <= 0 ? 20 : 200, page.getPageSize());
            assertEquals(size <= 0 ? 20 : 65, page.getData().size());
        }
    }

    @Test public void textTypeAndTimeFiltersApplyToBothCountAndRows() {
        QueryScanLogScheme query = new QueryScanLogScheme();
        query.setQrCode("mat"); query.setMsg("row-"); query.setMsgType((short) 0);
        query.setStartTime(late); query.setEndTime(late);
        PageData<ScanLogDTO> page = service.listByPage(query);
        assertEquals(12, page.getTotalPage());
        assertEquals(12, page.getData().size());
        assertTrue(page.getData().stream().allMatch(row -> row.getId() > 40 && row.getId() % 2 == 0));
    }

    @Test public void oneSidedTimeFiltersWork() {
        QueryScanLogScheme query = new QueryScanLogScheme();
        query.setStartTime(late);
        assertEquals(25, service.listByPage(query).getTotalPage());
        query.setStartTime(null); query.setEndTime(early);
        assertEquals(40, service.listByPage(query).getTotalPage());
    }
    @Test public void numericDeviceIdsMatchExactlyAndSnapshotTextCanBeSearched() {
        fixture.update("UPDATE scan_log SET scanner_id=1,plc_id=3,scanner_snapshot='Scanner [ID=1,上]',plc_snapshot='PLC_Up [ID=3,192.0.2.23]',status='FAILED' WHERE id=1");
        fixture.update("UPDATE scan_log SET scanner_id=10,plc_id=10,scanner_snapshot='Scanner [ID=10,位置5]',plc_snapshot='PLC_Up [ID=10,192.0.2.23]',status='FAILED' WHERE id=2");
        QueryScanLogScheme query=new QueryScanLogScheme();query.setScanner("1");query.setStatus("FAILED");
        assertEquals(1,service.listByPage(query).getTotalPage());
        assertEquals(Long.valueOf(1),service.listByPage(query).getData().get(0).getScannerId());
        query.setScanner("");query.setPlc("192.0.2.23");assertEquals(2,service.listByPage(query).getTotalPage());
        query.setPlc("10");assertEquals(1,service.listByPage(query).getTotalPage());
    }

    @Test public void emptyAndOutOfRangePagesDoNotReturnAllRows() {
        QueryScanLogScheme query = new QueryScanLogScheme();
        query.setCurrentPage(100);
        PageData<ScanLogDTO> page = service.listByPage(query);
        assertEquals(65, page.getTotalPage()); assertTrue(page.getData().isEmpty());
        query.setCurrentPage(1); query.setQrCode("missing");
        page = service.listByPage(query);
        assertEquals(0, page.getTotalPage()); assertTrue(page.getData().isEmpty());
    }
}
