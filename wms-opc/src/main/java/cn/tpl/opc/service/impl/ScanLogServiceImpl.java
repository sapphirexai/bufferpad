package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.dto.result.ScanLogDTO;
import cn.tpl.opc.commons.scheme.request.QueryScanLogScheme;
import cn.tpl.opc.entity.ScanLogEntity;
import cn.tpl.opc.mapper.ScanLogEntityMapper;
import cn.tpl.opc.service.IScanLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/11/8
 * 扫码日志服务
 */
@Service("scanLogService")
public class ScanLogServiceImpl implements IScanLogService {
    @Resource
    private ScanLogEntityMapper scanLogEntityMapper;

    @Override
    public void add(String qrCode, String msg, short msgType) {
        ScanLogEntity entity = new ScanLogEntity();
        entity.setQrCode(qrCode);
        entity.setMsg(qrCode);
        entity.setMsgType(msgType);
        scanLogEntityMapper.insertSelective(entity);
    }

    @Override
    public List<ScanLogDTO> listAllByScheme(QueryScanLogScheme scheme) {
        return scanLogEntityMapper.listAllByScheme(scheme).stream().map(this::scanLog2DTO).toList();
    }

    private ScanLogDTO scanLog2DTO(ScanLogEntity scanLog) {
        if (null == scanLog) return null;
        ScanLogDTO scanLogDTO = new ScanLogDTO();
        BeanUtil.copyProperties(scanLog, scanLogDTO);
        return scanLogDTO;
    }
}
