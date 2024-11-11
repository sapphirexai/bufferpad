package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.commons.dto.result.CushionDetailDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionDetailPageScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.entity.*;
import cn.tpl.opc.mapper.CushionDetailEntityMapper;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.service.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/17
 * 缓冲垫信息服务
 */
@Slf4j
@Service("cushionInfoService")
@Transactional
public class CushionInfoServiceImpl implements ICushionInfoService, InitializingBean, DisposableBean {
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private OpcConfigEntityMapper opcConfigEntityMapper;
    @Resource
    private CushionInfoEntityMapper cushionInfoEntityMapper;
    @Resource
    private CushionDetailEntityMapper cushionDetailEntityMapper;
    @Resource
    private ISseService sseService;
    @Resource
    private IDeviceInfoService deviceService;
    @Resource
    private IPLCAddrService plcAddrService;

    /**
     * 将当前所有扫码器的状态通知给PLC
     *
     * @param qrCode      缓冲垫二维码
     * @param plcAddrType PLC地址类型
     * @param workLine    产线
     */
    private void notifyAllScannersSates2PLC(String qrCode, int plcAddrType, Integer workLine) {
        List<DeviceInfoEntity> deviceInfoEntities = deviceService.listDeviceInfoByType(0);
        if (CollectionUtils.isEmpty(deviceInfoEntities)) return;

        for (DeviceInfoEntity deviceInfo : deviceInfoEntities) {
            Integer installSeq = deviceInfo.getInstallSeq();
            if (null == installSeq) continue;
            notifyPLC(qrCode, plcAddrType, installSeq, workLine);
        }
    }

    /**
     * 获取PLC地址类型
     *
     * @param isReScan   是否补码
     * @param scannerSeq 扫码器顺序
     * @return PLC地址类型
     */
    private static Integer getPlcAddrType(boolean isReScan, Integer scannerSeq) {
        Integer plcAddrType = null;
        if (Params.SCANNER_SEQ_KEY_1 == scannerSeq) {
            plcAddrType = Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_UP;
            if (isReScan)
                plcAddrType = Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_UP;
        }

        if (Params.SCANNER_SEQ_KEY_2 == scannerSeq) {
            plcAddrType = Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_DOWN;
            if (isReScan)
                plcAddrType = Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_DOWN;
        }
        return plcAddrType;
    }

    /**
     * 收到新的二维码时
     */
    private ResultDTO<CushionInfoDTO> onScanNew(Integer workLine, Integer scannerSeq, String qrCode) {
        boolean addResult = add(workLine, scannerSeq, qrCode);
        log.info("onQrCodeReceived，新增缓冲垫结果：[{}]", addResult);
        if (addResult) {
            addDetail(cushionInfoEntityMapper.findByQrCode(qrCode));
            // 扫码成功PLC提示
            if (null == scannerSeq)
                notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
            else
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, scannerSeq, workLine);

            return ResultDTO.success(onScanCodeSuccess(workLine, qrCode, scannerSeq));
        }
        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, null, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_ADD_FAILED);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_FAILED);
    }

    /**
     * 无效扫码时
     */
    private ResultDTO<CushionInfoDTO> onScanIneffective(Integer workLine, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        Integer cushionScannerSeq = cushionInfoEntity.getScannerSeq();
        String qrCode = cushionInfoEntity.getQrCode();
        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_INVALID_SCAN);
        // 扫码成功PLC提示
        if (null == scannerSeq) {
            // 补码逻辑
            if (null == cushionScannerSeq)
                notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
            else
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, cushionScannerSeq, workLine);
        } else {
            notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, scannerSeq, workLine);
        }
        scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_INVALID_DATA_FORM_SCANNER + scannerSeq, Constants.SCAN_LOG_TYPE_ERROR);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_INVALID_SCAN);
    }

    /**
     * 达到使用最大次数时
     */
    private void onScanMax(Integer workLine, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        Integer cushionScannerSeq = cushionInfoEntity.getScannerSeq();
        String qrCode = cushionInfoEntity.getQrCode();
        if (null == scannerSeq) {
            // 补码逻辑
            if (null == cushionScannerSeq)
                notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM, workLine);
            else
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM, cushionScannerSeq, workLine);
        } else {
            notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM, scannerSeq, workLine);
        }
        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX);
    }


    /**
     * 二维码收到时的处理
     * 由于手动补码时没有scannerSeq
     * 如果当前缓冲垫存在且已经记录scannerSeq就用缓冲垫的scannerSeq
     * 否则把所有扫码器的状态给到PLC
     */
    @Override
    public ResultDTO<CushionInfoDTO> onQrCodeReceived(Integer workLine, Integer scannerSeq, String qrCode) {
        scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_SUCCESS_DATA_FORM_SCANNER + scannerSeq, Constants.SCAN_LOG_TYPE_INFO);
        CushionInfoEntity cushionInfoEntity = findByQrCode(qrCode);
        if (null == cushionInfoEntity) return onScanNew(workLine, scannerSeq, qrCode);

        //若当前与最后一次扫码时间相差不足2小时，则为无效扫码，不进行记录操作
        Date lastScanDate = cushionInfoEntity.getLastScanDate();
        long interval = System.currentTimeMillis() - lastScanDate.getTime();
        if (interval < Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS) {
            log.info("handleScannerData, onScanIneffective, interval => {}ms", interval);
            return onScanIneffective(workLine, scannerSeq, cushionInfoEntity);
        }

        // 增加当前缓冲垫1次使用次数
        cushionInfoEntity.setUsedCount(cushionInfoEntity.getUsedCount() + 1);
        boolean modifyResult = modifyUsedCountByQrCode(cushionInfoEntity, scannerSeq);
        log.info("handleScannerData, modify usedCount, result => [{}]", modifyResult);
        if (modifyResult) {
            // 扫码成功PLC提示
            return onScanSuccess(workLine, scannerSeq, cushionInfoEntity);
        }

        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
    }

    /**
     * 扫码成功时
     */
    private ResultDTO<CushionInfoDTO> onScanSuccess(Integer workLine, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        Integer cushionScannerSeq = cushionInfoEntity.getScannerSeq();
        String qrCode = cushionInfoEntity.getQrCode();
        addDetail(cushionInfoEntity);
        if (null == scannerSeq) {
            // 补码逻辑
            if (null == cushionScannerSeq)
                notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
            else
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, cushionScannerSeq, workLine);

            return ResultDTO.success(onScanCodeSuccess(workLine, qrCode, cushionScannerSeq));
        } else {
            notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, scannerSeq, workLine);
        }

        return ResultDTO.success(onScanCodeSuccess(workLine, qrCode, scannerSeq));
    }

    /**
     * 缓冲垫扫码成功
     *
     * @param workLine      产线
     * @param cushionQrCode 缓冲垫二维码
     * @param scannerSeq    扫码器安装顺序
     */
    private CushionInfoDTO onScanCodeSuccess(Integer workLine, String cushionQrCode, Integer scannerSeq) {
        if (StringUtils.isEmpty(cushionQrCode)) return null;
        CushionInfoEntity cushionInfoEntity = findByQrCode(cushionQrCode);
        if (null == cushionInfoEntity) return null;
        log.info("onScanCodeSuccess");
        CushionInfoDTO cushionInfoDTO = cushionInfo2DTO(cushionInfoEntity);
        cushionInfoDTO.setScannerSeq(scannerSeq);
        sseService.sendCushionMsg(cushionInfoDTO);

        int maxUseCount = cushionInfoEntity.getMaxUseCount();
        int usedCount = cushionInfoEntity.getUsedCount();
        // 达到最大次数PLC报警
        if (maxUseCount <= usedCount) {
            log.warn("handleScannerData，onScanMax，maxUseCount => {}，usedCount => {}", maxUseCount, usedCount);
            scanLogService.add(cushionQrCode, Constants.SCAN_LOG_MSG_OVER_MAXIMUM_FORM_SCANNER + scannerSeq, Constants.SCAN_LOG_TYPE_ERROR);
            scanLogService.add(cushionQrCode, Constants.SCAN_LOG_MSG_PREFIX_CURRENT_COUNT + usedCount + Constants.SCAN_LOG_MSG_SUFFIX_MAX_COUNT + maxUseCount, Constants.SCAN_LOG_TYPE_ERROR);
            onScanMax(workLine, scannerSeq, cushionInfoEntity);
        }

        return cushionInfoDTO;
    }

    /**
     * 缓冲垫扫码失败
     *
     * @param workLine   产线
     * @param scannerSeq 扫码器安装顺序
     */
    private void onScanCodeFailed(Integer workLine, Integer scannerSeq) {
        log.info("onScanCodeFailed");
        scanLogService.add(null, Constants.SCAN_LOG_MSG_FAILED, Constants.SCAN_LOG_TYPE_ERROR);
        notifyPLC(null, Constants.PLC_ADDR_TYPE_SCAN_FAILED, scannerSeq, workLine);
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        cushionInfoDTO.setWorkLine(workLine);
        cushionInfoDTO.setScannerSeq(scannerSeq);
        sseService.sendCushionMsg(cushionInfoDTO);// 推送一条缓冲垫数据到客户端
    }

    @Override
    public PageData<CushionInfoDTO> listByPage(QueryCushionInfoPageScheme scheme) {
        Page<CushionInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<CushionInfoEntity> iPage = cushionInfoEntityMapper.listByPage(page, scheme);
        return PageData.of(iPage, this::cushionInfo2DTO);
    }


    private CushionInfoDTO cushionInfo2DTO(CushionInfoEntity cushionInfo) {
        if (null == cushionInfo) return null;
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtil.copyProperties(cushionInfo, cushionInfoDTO);
        return cushionInfoDTO;
    }

    private CushionDetailDTO cushionDetail2DTO(CushionDetailEntity cushionDetail) {
        if (null == cushionDetail) return null;
        CushionDetailDTO cushionDetailDTO = new CushionDetailDTO();
        BeanUtil.copyProperties(cushionDetail, cushionDetailDTO);
        return cushionDetailDTO;
    }

    @Override
    public boolean add(Integer workLine, Integer scannerSeq, String qrCode) {
        // 二维码为空直接返回失败
        if (StringUtils.isEmpty(qrCode)) return false;

        OpcConfigEntity opcConfig = opcConfigEntityMapper.selectByPrimaryKey(Constants.OPC_CONFIG_ID);
        CushionInfoEntity cushionInfoEntity = new CushionInfoEntity();
        cushionInfoEntity.setWorkLine(workLine);
        cushionInfoEntity.setQrCode(qrCode);
        if (null != opcConfig)
            cushionInfoEntity.setMaxUseCount(opcConfig.getCushionMaxUseCount());
        else
            cushionInfoEntity.setMaxUseCount(Constants.CUSHION_DEFAULT_MAX_USE_CONT);
        cushionInfoEntity.setUsedCount(Constants.CUSHION_ADD_DEFAULT_USED_COUNT);
        cushionInfoEntity.setLastScanDate(new Date());
        cushionInfoEntity.setScannerSeq(scannerSeq);
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
    public boolean modifyUsedCountByQrCode(CushionInfoEntity cushionInfoEntity, Integer scannerSeq) {
        cushionInfoEntity.setLastScanDate(new Date());
        cushionInfoEntity.setScannerSeq(scannerSeq);
        return cushionInfoEntityMapper.modifyUsedCountByQrCode(cushionInfoEntity) > 0;
    }

    /**
     * 添加对应缓冲垫数据明细
     *
     * @param cushionInfo 缓冲垫数据
     */
    private void addDetail(CushionInfoEntity cushionInfo) {
        CushionDetailEntity cushionDetail = new CushionDetailEntity();
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreProperties("id", "modifiedDate");
        BeanUtil.copyProperties(cushionInfo, cushionDetail, copyOptions);
        cushionDetail.setCreatedDate(new Date());
        cushionDetailEntityMapper.insertSelective(cushionDetail);
    }

    @Override
    public boolean modifyMaxUseCountByIds(ModifyCushionInfoScheme scheme) {
        return cushionInfoEntityMapper.modifyMaxUseCountByIds(scheme) > 0;
    }

    @Override
    public void afterPropertiesSet() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    /**
     * 通知PLC扫码结果
     *
     * @param qrCode      缓冲垫二维码
     * @param plcAddrType PLC寄存器地址类型
     * @param scannerSeq  扫码器安装顺序
     * @param workLine    产线
     */
    private void notifyPLC(String qrCode, Integer plcAddrType, Integer scannerSeq, Integer workLine) {
        if (null == plcAddrType) return;

        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerSeq(plcAddrType, scannerSeq);
        if (null == plcAddr) return;

        EventBus.getDefault().post(new EventBusMsgPlcCmd(qrCode, plcAddrType, plcAddr.getAddr(), Constants.DEFAULT_2_PLC_VAL, workLine));

        if (Constants.PLC_ADDR_TYPE_SCAN_SUCCESS == plcAddrType)
            readOpenCountFromPLC(false, qrCode, scannerSeq, workLine);

        if (Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS == plcAddrType)
            readOpenCountFromPLC(true, qrCode, scannerSeq, workLine);
    }

    @Override
    public boolean modifyOpenCountByQrCode(String qrCode, Short openCount) {
        if (null == openCount) return false;

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

    /**
     * 从PLC读取开口数
     *
     * @param isReScan   是否为重新扫码
     * @param qrCode     缓冲垫二维码
     * @param scannerSeq 扫码器安装顺序
     * @param workLine   产线
     */
    private void readOpenCountFromPLC(boolean isReScan, String qrCode, Integer scannerSeq, Integer workLine) {
        // 根据不同的缓冲垫位置来源读取
        log.info("readOpenCountFromPLC, isReScan => {}, seq => {}", isReScan, scannerSeq);
        if (null == scannerSeq) return;

        Integer plcAddrType = getPlcAddrType(isReScan, scannerSeq);

        if (null == plcAddrType) return;

        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerSeq(plcAddrType, scannerSeq);

        if (null == plcAddr) return;

        EventBus.getDefault().post(new EventBusMsgReadOpenCountFromPLC(qrCode, plcAddr.getAddr(), workLine));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgCushionQrCode event) {
        log.info("onMessageEvent, qrCode => {}", event);
        String qrCode = event.getQrCode();
        Integer workLine = event.getWorkLine();
        Integer scannerSeq = event.getScannerSeq();
        if (StringUtils.isEmpty(qrCode)) {
            onScanCodeFailed(workLine, scannerSeq);
            return;
        }
        onQrCodeReceived(workLine, scannerSeq, qrCode);
    }
}
