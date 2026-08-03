package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.application.scan.ScanApplicationService;
import cn.tpl.opc.application.scan.ScanCommand;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionDetailDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionDetailPageScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.entity.CushionDetailEntity;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.entity.OpcConfigEntity;
import cn.tpl.opc.mapper.CushionDetailEntityMapper;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service("cushionInfoService")
@Transactional
public class CushionInfoServiceImpl implements ICushionInfoService {
    @Resource
    private ScanApplicationService scanApplicationService;
    @Resource
    private OpcConfigEntityMapper opcConfigEntityMapper;
    @Resource
    private CushionInfoEntityMapper cushionInfoEntityMapper;
    @Resource
    private CushionDetailEntityMapper cushionDetailEntityMapper;

    @Override
    public ResultDTO<CushionInfoDTO> onQrCodeReceived(Long scannerId, Integer workLine, String scannerHost, String scannerName, String scannerPosition, Integer scannerSeq, String qrCode) {
        return onQrCodeReceived(null, scannerId, workLine, scannerHost, scannerName, scannerPosition, scannerSeq, qrCode);
    }

    @Override
    public ResultDTO<CushionInfoDTO> onQrCodeReceived(String operationId, Long scannerId, Integer workLine,
                                                      String scannerHost, String scannerName,
                                                      String scannerPosition, Integer scannerSeq, String qrCode) {
        return scanApplicationService.handleScan(new ScanCommand(operationId, scannerId, workLine, scannerHost,
                scannerName, scannerPosition, scannerSeq, qrCode));
    }

    @Override
    public PageData<CushionInfoDTO> listByPage(QueryCushionInfoPageScheme scheme) {
        Page<CushionInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<CushionInfoEntity> iPage = cushionInfoEntityMapper.listByPage(page, scheme);
        return PageData.of(iPage, this::cushionInfo2DTO);
    }

    @Override
    public boolean add(Integer workLine, String scannerPosition, Long scannerId, Integer scannerSeq, String qrCode) {
        if (StringUtils.isEmpty(qrCode)) return false;

        OpcConfigEntity opcConfig = opcConfigEntityMapper.selectByPrimaryKey(Constants.OPC_CONFIG_ID);
        CushionInfoEntity cushionInfoEntity = new CushionInfoEntity();
        cushionInfoEntity.setWorkLine(workLine);
        cushionInfoEntity.setQrCode(qrCode);
        cushionInfoEntity.setMaxUseCount(opcConfig == null ? Constants.CUSHION_DEFAULT_MAX_USE_CONT : opcConfig.getCushionMaxUseCount());
        cushionInfoEntity.setUsedCount(Constants.CUSHION_ADD_DEFAULT_USED_COUNT);
        cushionInfoEntity.setLastScanDate(new Date());
        cushionInfoEntity.setScannerId(scannerId);
        cushionInfoEntity.setScannerSeq(scannerSeq);
        cushionInfoEntity.setScannerPosition(scannerPosition);
        return cushionInfoEntityMapper.insertSelective(cushionInfoEntity) > 0;
    }

    @Override
    public CushionInfoEntity findByQrCode(String qrCode) {
        return cushionInfoEntityMapper.findByQrCode(qrCode);
    }

    @Override
    public List<CushionInfoDTO> listByQrCode(String qrCode) {
        List<CushionInfoEntity> cushionInfos = cushionInfoEntityMapper.listByQrCode(qrCode);
        return cushionInfos.stream().map(this::cushionInfo2DTO).collect(Collectors.toList());
    }

    @Override
    public PageData<CushionDetailDTO> listDetailsByPage(QueryCushionDetailPageScheme scheme) {
        Page<CushionDetailEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<CushionDetailEntity> iPage = cushionDetailEntityMapper.listByPage(page, scheme);
        return PageData.of(iPage, this::cushionDetail2DTO);
    }

    @Override
    public List<CushionInfoDTO> listByIds(List<Long> ids) {
        List<CushionInfoEntity> cushionInfos = cushionInfoEntityMapper.listByIds(ids);
        return cushionInfos.stream().map(this::cushionInfo2DTO).collect(Collectors.toList());
    }

    @Override
    public List<CushionDetailDTO> listDetailsByIds(List<Long> ids) {
        List<CushionDetailEntity> cushionDetails = cushionDetailEntityMapper.listByIds(ids);
        return cushionDetails.stream().map(this::cushionDetail2DTO).collect(Collectors.toList());
    }

    @Override
    public boolean modifyUsedCountByQrCode(CushionInfoEntity cushionInfoEntity, String scannerPosition, Long scannerId, Integer scannerSeq) {
        Date scanDate = new Date();
        Date lastScanDateBefore = new Date(scanDate.getTime() - Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS);
        cushionInfoEntity.setLastScanDate(scanDate);
        cushionInfoEntity.setScannerPosition(scannerPosition);
        cushionInfoEntity.setScannerId(scannerId);
        cushionInfoEntity.setScannerSeq(scannerSeq);
        return cushionInfoEntityMapper.modifyUsedCountByQrCode(cushionInfoEntity, lastScanDateBefore) > 0;
    }

    @Override
    public boolean modifyOpenCountByQrCode(String qrCode, Short openCount) {
        if (openCount == null) return false;

        boolean modifyDetailResult = false;
        CushionInfoEntity cushionInfo = new CushionInfoEntity();
        cushionInfo.setQrCode(qrCode);
        cushionInfo.setOpenCount(openCount);

        boolean modifyInfoResult = cushionInfoEntityMapper.modifyOpenCountByQrCode(cushionInfo) > 0;
        if (modifyInfoResult) {
            CushionDetailEntity cushionDetail = new CushionDetailEntity();
            BeanUtil.copyProperties(cushionInfo, cushionDetail);
            modifyDetailResult = cushionDetailEntityMapper.modifyOpenCountByQrCode(cushionDetail) > 0;
        }

        return modifyInfoResult && modifyDetailResult;
    }

    @Override
    public boolean modifyMaxUseCountByIds(ModifyCushionInfoScheme scheme) {
        return cushionInfoEntityMapper.modifyMaxUseCountByIds(scheme) > 0;
    }

    private CushionInfoDTO cushionInfo2DTO(CushionInfoEntity cushionInfo) {
        if (cushionInfo == null) return null;
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtil.copyProperties(cushionInfo, cushionInfoDTO);
        return cushionInfoDTO;
    }

    private CushionDetailDTO cushionDetail2DTO(CushionDetailEntity cushionDetail) {
        if (cushionDetail == null) return null;
        CushionDetailDTO cushionDetailDTO = new CushionDetailDTO();
        BeanUtil.copyProperties(cushionDetail, cushionDetailDTO);
        return cushionDetailDTO;
    }
}
