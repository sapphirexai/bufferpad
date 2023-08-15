package cn.tpl.opc.service.impl;

import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.mapper.PLCAddrEntityMapper;
import cn.tpl.opc.service.IPLCAddrService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 * PLC寄存器地址服务
 */
@Service("plcAddrService")
public class PLCAddrServiceImpl implements IPLCAddrService {
    @Resource
    private PLCAddrEntityMapper plcAddrEntityMapper;

    @Override
    public PLCAddrEntity findByTypeAndScannerSeq(Integer type, Integer scannerSeq) {
        if (null == type || null == scannerSeq) return null;

        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setType(type);
        entity.setScannerSeq(scannerSeq);
        return plcAddrEntityMapper.findByTypeAndScannerSeq(entity);
    }
}
