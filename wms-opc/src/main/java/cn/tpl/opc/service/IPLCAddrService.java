package cn.tpl.opc.service;

import cn.tpl.opc.entity.PLCAddrEntity;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 * PLC寄存器地址服务
 */
public interface IPLCAddrService {
    PLCAddrEntity findByTypeAndScannerSeq(Integer type, Integer scannerSeq);
}
