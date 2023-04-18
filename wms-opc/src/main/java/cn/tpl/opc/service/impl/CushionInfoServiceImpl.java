package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/17
 */
@Service("cushionInfoService")
public class CushionInfoServiceImpl implements ICushionInfoService {
    @Resource
    private CushionInfoEntityMapper cushionInfoEntityMapper;

    @Override
    public boolean add(String qrCode) {
        // 二维码为空直接返回失败
        if (StringUtils.isEmpty(qrCode)) return false;

        int maxUseCount = 100;
        if (qrCode.startsWith(Constants.CUSHION_INFO_QR_CODE_PREFIX_T))
            maxUseCount = 150;

        CushionInfoEntity cushionInfoEntity = new CushionInfoEntity();
        cushionInfoEntity.setQrCode(qrCode);
        cushionInfoEntity.setMaxUseCount(maxUseCount);
        cushionInfoEntity.setUsedCount(Constants.CUSHION_INGO_ADD_DEFAULT_USED_COUNT);
        cushionInfoEntity.setLastScanDate(new Date());
        return cushionInfoEntityMapper.insert(cushionInfoEntity) > 0;
    }

    @Override
    public CushionInfoEntity findByQrCode(String qrCode) {
        return cushionInfoEntityMapper.findByQrCode(qrCode);
    }

    @Override
    public boolean modifyUsedCountByQrCode(String qrCode, Integer count) {
        if (null == count) return false;
        CushionInfoEntity cushionInfo = new CushionInfoEntity();
        cushionInfo.setQrCode(qrCode);
        cushionInfo.setUsedCount(count);
        cushionInfo.setLastScanDate(new Date());
        return cushionInfoEntityMapper.modifyUsedCountByQrCode(cushionInfo) > 0;
    }
}
