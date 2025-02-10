package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.ScanLogDTO;
import cn.tpl.opc.commons.scheme.request.QueryScanLogScheme;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/11/8
 * 扫码日志服务接口
 */
public interface IScanLogService {

    /**
     * 新增日志
     *
     * @param qrCode  缓冲垫二维码
     * @param msg     日志内容
     * @param msgType 日志类型
     * @see cn.tpl.opc.commons.constant.Constants#SCAN_LOG_TYPE_INFO
     * @see cn.tpl.opc.commons.constant.Constants#SCAN_LOG_TYPE_ERROR
     */
    void add(String qrCode, String msg, short msgType);

    /**
     * 新增日志
     *
     * @param scannerHost 扫码器地址
     * @param scannerName 扫码器名字
     * @param qrCode  缓冲垫二维码
     * @param msg 日志内容，可为null，默认：扫码成功或扫码失败
     * @param msgType 日志类型
     * @param isManualScan 是否手动扫码
     *
     * @see cn.tpl.opc.commons.constant.Constants#SCAN_LOG_TYPE_INFO
     * @see cn.tpl.opc.commons.constant.Constants#SCAN_LOG_TYPE_ERROR
     */
    void addScanLog(String scannerHost, String scannerName, String qrCode, String msg, short msgType, boolean isManualScan);

    /**
     * 根据ID列表查找对应缓冲垫明细
     *
     * @param scheme 分页查询参数协议
     * @return 分页数据
     */
    List<ScanLogDTO> listAllByScheme(QueryScanLogScheme scheme);
}
