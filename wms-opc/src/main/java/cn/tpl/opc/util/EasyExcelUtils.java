package cn.tpl.opc.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2020/7/30
 * EasyExcel操作类
 */
public class EasyExcelUtils {
    private EasyExcelUtils() {
    }

    /**
     * 导出
     *
     * @param target      输出目标
     * @param exportDatas 导出数据
     * @param template    模板类
     * @param <T>         导出数据类型
     */
    public static <T> void export(String target, List<T> exportDatas, Class<?> template) {
        doExport(EasyExcel.write(target, template), exportDatas);
    }

    public static <T> void export(String target, List<T> exportDatas, Class<?> template, ExcelTypeEnum excelTypeEnum) {
        doExport(EasyExcel.write(target, template), exportDatas, excelTypeEnum);
    }

    public static <T> void export(OutputStream target, List<T> exportDatas, Class<?> template) {
        doExport(EasyExcel.write(target, template), exportDatas);
    }

    public static <T> void export(File target, List<T> exportDatas, Class<?> template) {
        doExport(EasyExcel.write(target, template), exportDatas);
    }

    private static <T> void doExport(ExcelWriterBuilder builder, List<T> exportDatas) {
        builder.needHead(true)
                .excelType(ExcelTypeEnum.XLS)
                .sheet()
                .doWrite(exportDatas);
    }

    private static <T> void doExport(ExcelWriterBuilder builder, List<T> exportDatas, ExcelTypeEnum excelTypeEnum) {
        builder.needHead(true)
                .excelType(excelTypeEnum)
                .sheet()
                .doWrite(exportDatas);
    }
}
