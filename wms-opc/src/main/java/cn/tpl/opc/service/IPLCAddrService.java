package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.PLCAddrDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryPLCAddrPageScheme;
import cn.tpl.opc.commons.scheme.request.SavePLCAddrScheme;
import cn.tpl.opc.entity.PLCAddrEntity;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 * PLC寄存器地址服务
 */
public interface IPLCAddrService {
    PLCAddrEntity findByTypeAndScannerId(Integer type, Long scannerId);

    List<PLCAddrEntity> listByPlcIdAndType(Long plcId, Integer type);

    PageData<PLCAddrDTO> listByPage(QueryPLCAddrPageScheme scheme);

    PLCAddrDTO findById(Long id);

    boolean save(SavePLCAddrScheme scheme);

    boolean deleteById(Long id);
}
