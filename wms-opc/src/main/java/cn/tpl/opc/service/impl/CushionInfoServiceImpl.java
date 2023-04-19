package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
    public PageData<CushionInfoDTO> listByPage(QueryCushionInfoPageScheme scheme) {
        Page<CushionInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<CushionInfoEntity> iPage = cushionInfoEntityMapper.listByPage(page);
        List<CushionInfoEntity> cushionInfos = iPage.getRecords();

        if (CollectionUtils.isEmpty(cushionInfos)) return null;
        return PageData.of(iPage, this::cushionInfo2DTO);
    }


    private CushionInfoDTO cushionInfo2DTO(CushionInfoEntity cushionInfo) {
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtils.copyProperties(cushionInfo, cushionInfoDTO);
        return cushionInfoDTO;
    }

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
