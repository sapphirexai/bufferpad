package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.result.ExportCushionInfoDTO;
import cn.tpl.opc.commons.scheme.request.ExportCushionsByTimeScheme;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.nio.file.*;
import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CushionTimeExportService {
    private final DataSource dataSource;
    private final int maxRows;
    public CushionTimeExportService(DataSource dataSource, @Value("${cushion.export.max-rows:100000}") int maxRows) {
        if (maxRows<1 || maxRows>1000000) throw new IllegalArgumentException("Invalid cushion.export.max-rows");
        this.dataSource=dataSource;this.maxRows=maxRows;
    }
    public record ExportFile(Path path, long rows) implements AutoCloseable {
        @Override public void close() throws IOException { Files.deleteIfExists(path); }
    }

    public ExportFile generate(ExportCushionsByTimeScheme scheme) throws IOException, SQLException {
        ExportCushionsByTimeScheme.Range range=scheme.validatedRange();
        Path path=Files.createTempFile("bufferpad-cushion-time-", ".xlsx");
        boolean complete=false;
        try {
            long count=writeFile(path,range);
            complete=true;
            return new ExportFile(path,count);
        } finally { if(!complete)Files.deleteIfExists(path); }
    }

    private long writeFile(Path path, ExportCushionsByTimeScheme.Range range) throws IOException, SQLException {
        try (Connection c=dataSource.getConnection()) {
            boolean auto=c.getAutoCommit(),readOnly=c.isReadOnly();int isolation=c.getTransactionIsolation();
            String timeZone=null;
            try {
                // Explicit business wall time for TIMESTAMP fields as well as DATETIME fields.
                try (Statement s=c.createStatement()) {
                    s.setQueryTimeout(10);
                    try (ResultSet rs=s.executeQuery("SELECT @@SESSION.time_zone")) { rs.next();timeZone=rs.getString(1); }
                    s.execute("SET SESSION time_zone='+08:00'");
                }
                c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);c.setAutoCommit(false);
                Map<Integer,String> positions=new HashMap<>();
                try (Statement s=c.createStatement()) {
                    s.setQueryTimeout(10);
                    try (ResultSet rs=s.executeQuery("SELECT id,name FROM device_install_position")) { while(rs.next())positions.put(rs.getInt(1),rs.getString(2)); }
                }
                String sql="SELECT id,qr_code,max_use_count,used_count,open_count,scanner_position,scanner_seq,"
                        +"DATE_FORMAT(created_date,'%Y-%m-%d %H:%i:%s') AS created_text,"
                        +"DATE_FORMAT(last_scan_date,'%Y-%m-%d %H:%i:%s') AS last_text FROM cushion_info WHERE "
                        +range.column()+">=? AND "+range.column()+"<? AND (? IS NULL OR id>?) ORDER BY id LIMIT 1000";
                long count=0;Long last=null;
                long started=System.nanoTime();
                try (PreparedStatement s=c.prepareStatement(sql); ExcelWriter writer=EasyExcel.write(path.toFile(),ExportCushionInfoDTO.class).build()) {
                    s.setQueryTimeout(30);
                    DateTimeFormatter format=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    s.setString(1,range.start().format(format));s.setString(2,range.endExclusive().format(format));
                    WriteSheet sheet=EasyExcel.writerSheet("缓冲垫信息").build();
                    while (true) {
                        if ((System.nanoTime()-started)/1000000000L>=120) throw new IllegalArgumentException("导出耗时过长，请缩小时间范围");
                        if (last==null) { s.setNull(3,Types.BIGINT);s.setNull(4,Types.BIGINT); }
                        else { s.setLong(3,last);s.setLong(4,last); }
                        List<ExportCushionInfoDTO> rows=new ArrayList<>();
                        try (ResultSet rs=s.executeQuery()) {
                            while(rs.next()) {
                                last=rs.getLong("id");count++;
                                if (count>maxRows) throw new IllegalArgumentException("导出数据超过"+maxRows+"条，请缩小时间范围");
                                ExportCushionInfoDTO row=new ExportCushionInfoDTO();
                                row.setQrCode(rs.getString("qr_code"));
                                Number max=(Number)rs.getObject("max_use_count"),used=(Number)rs.getObject("used_count");
                                row.setMaxUseCount(max==null?null:max.intValue());row.setUsedCount(used==null?null:used.intValue());
                                Number open=(Number)rs.getObject("open_count");row.setOpenCount(open==null?null:open.shortValue());
                                String position=rs.getString("scanner_position");
                                row.setScannerPosition(position==null || position.isEmpty()?positions.getOrDefault(rs.getInt("scanner_seq"),""):position);
                                row.setCreatedDate(rs.getString("created_text"));row.setLastScanDate(rs.getString("last_text"));rows.add(row);
                            }
                        }
                        if (rows.isEmpty()) break;
                        writer.write(rows,sheet);
                        if (rows.size()<1000)break;
                    }
                }
                if (count==0) throw new IllegalArgumentException("所选时间段没有缓冲垫信息");
                c.rollback();
                return count;
            } finally {
                try { c.rollback(); }
                finally {
                    c.setReadOnly(readOnly);c.setTransactionIsolation(isolation);c.setAutoCommit(auto);
                    if (timeZone!=null) try (PreparedStatement s=c.prepareStatement("SET SESSION time_zone=?")) {s.setString(1,timeZone);s.execute();}
                }
            }
        }
    }
}
