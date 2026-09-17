package cn.tpl.opc.commons.scheme.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

@Data
public class ExportCushionsByTimeScheme {
    private String timeType = "LAST_USE";
    private String startTime;
    private String endTime;
    public record Range(String column, LocalDateTime start, LocalDateTime endExclusive) {}
    public Range validatedRange() {
        String column;
        if ("FIRST_USE".equals(timeType)) column="created_date";
        else if ("LAST_USE".equals(timeType)) column="last_scan_date";
        else throw new IllegalArgumentException("请选择第一次使用时间或最后一次使用时间");
        DateTimeFormatter format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDateTime start=LocalDateTime.parse(startTime,format), end=LocalDateTime.parse(endTime,format);
            if (start.isAfter(end)) throw new IllegalArgumentException("开始时间不能晚于结束时间");
            if (start.getYear()<1000 || end.getYear()>9998) throw new IllegalArgumentException("时间超出支持范围");
            return new Range(column,start,end.plusSeconds(1));
        } catch (java.time.format.DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("请选择有效的起止时间，格式为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
