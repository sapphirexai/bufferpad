package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.result.ScanLogDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryScanLogScheme;
import cn.tpl.opc.entity.ScanLogEntity;
import cn.tpl.opc.mapper.ScanLogEntityMapper;
import cn.tpl.opc.service.IScanLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/11/8
 * 扫码日志服务
 */
@Slf4j
@Service("scanLogService")
public class ScanLogServiceImpl implements IScanLogService {
    public static final String
            LOG_DIVIDER = "，",
            SCAN_MANUAL = "手动";

    @Resource
    private ScanLogEntityMapper scanLogEntityMapper;

    @Override
    public void add(String qrCode, String msg, short msgType) {
        log.info("add, qrCode:{}, msg:{}, msgType:{}", qrCode, msg, msgType);
        ScanLogEntity entity = new ScanLogEntity();
        entity.setQrCode(qrCode);
        entity.setMsg(msg);
        entity.setMsgType(msgType);
        scanLogEntityMapper.insertSelective(entity);
    }

    @Override
    public void addScanLog(String scannerHost, String scannerName, String qrCode, String msg, short msgType, boolean isManualScan) {
        log.info("addScanLog");
        String scanResult = CharSequenceUtil.isEmpty(msg) ? (msgType == Constants.SCAN_LOG_TYPE_INFO ? Constants.SCAN_LOG_MSG_SUCCESS : Constants.SCAN_LOG_MSG_FAILED) : msg;
        scanResult += LOG_DIVIDER;
        String scanMethod = isManualScan ? SCAN_MANUAL : scannerName + LOG_DIVIDER + scannerHost;
        add(qrCode, scanResult + scanMethod, msgType);
    }

    @Override
    public PageData<ScanLogDTO> listByPage(QueryScanLogScheme scheme) {
        // A negative MyBatis-Plus page size disables pagination; never allow an unbounded log read.
        int pageSize = scheme.getPageSize() <= 0 ? 20 : Math.min(scheme.getPageSize(), 200);
        Page<ScanLogEntity> page = new Page<>(Math.max(1, scheme.getCurrentPage()), pageSize);
        return PageData.of(scanLogEntityMapper.listByPage(page, scheme), this::scanLog2DTO);
    }

    private ScanLogDTO scanLog2DTO(ScanLogEntity scanLog) {
        if (null == scanLog) return null;
        ScanLogDTO scanLogDTO = new ScanLogDTO();
        BeanUtil.copyProperties(scanLog, scanLogDTO);
        return scanLogDTO;
    }
}
