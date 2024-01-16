package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.OpcConfigEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.ISseService;
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
public class CushionInfoServiceImpl implements ICushionInfoService, InitializingBean, DisposableBean {
    @Resource
    private OpcConfigEntityMapper opcConfigEntityMapper;
    @Resource
    private CushionInfoEntityMapper cushionInfoEntityMapper;
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
     * 二维码收到时的处理
     * 由于手动补码时没有scannerSeq
     * 如果当前缓冲垫存在且已经记录scannerSeq就用缓冲垫的scannerSeq
     * 否则把所有扫码器的状态给到PLC
     */
    @Override
    public ResultDTO<CushionInfoDTO> onQrCodeReceived(Integer workLine, Integer scannerSeq, String qrCode) {
        CushionInfoEntity cushionInfoEntity = findByQrCode(qrCode);
        if (null == cushionInfoEntity) {
            boolean addResult = add(workLine, scannerSeq, qrCode);
            log.info("onQrCodeReceived，新增缓冲垫结果：[{}]", addResult);
            if (addResult) {
                // 扫码成功PLC提示
                if (null == scannerSeq)
                    notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
                else
                    notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, scannerSeq, workLine);

                return ResultDTO.success(onScanCodeSuccess(qrCode, scannerSeq));
            }
            sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, null, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_ADD_FAILED);
            return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_FAILED);
        }

        Integer cushionScannerSeq = cushionInfoEntity.getScannerSeq();

        //若当前与最后一次扫码时间相差不足2小时，则为无效扫码，不进行记录操作
        Date lastScanDate = cushionInfoEntity.getLastScanDate();
        long interval = System.currentTimeMillis() - lastScanDate.getTime();
        if (interval < Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS) {
            log.info("handleScannerData，无效扫码，不进行操作，当前扫码间隔：{}毫秒", interval);
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

            return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_INVALID_SCAN);
        }

        int maxUseCount = cushionInfoEntity.getMaxUseCount();
        int usedCount = cushionInfoEntity.getUsedCount();

        // 超次数PLC报警
        if (maxUseCount <= usedCount) {
            log.warn("handleScannerData，缓冲垫使用次数已达极限，最大使用次数：{}，已使用次数：{}", maxUseCount, usedCount);
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
            return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX);
        }
        // 增加当前缓冲垫1次使用次数
        usedCount++;
        boolean modifyResult = modifyUsedCountByQrCode(qrCode, scannerSeq, usedCount);
        log.info("handleScannerData，增加缓冲垫已使用次数结果：[{}]", modifyResult);
        if (modifyResult) {
            // 扫码成功PLC提示
            if (null == scannerSeq) {
                // 补码逻辑
                if (null == cushionScannerSeq)
                    notifyAllScannersSates2PLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
                else
                    notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, cushionScannerSeq, workLine);

                return ResultDTO.success(onScanCodeSuccess(qrCode, cushionScannerSeq));
            } else {
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, scannerSeq, workLine);
            }
            return ResultDTO.success(onScanCodeSuccess(qrCode, scannerSeq));
        }

        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, workLine, scannerSeq), Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
    }

    /**
     * 缓冲垫扫码成功
     *
     * @param cushionQrCode 缓冲垫二维码
     * @param scannerSeq    扫码器安装顺序
     */
    private CushionInfoDTO onScanCodeSuccess(String cushionQrCode, Integer scannerSeq) {
        if (StringUtils.isEmpty(cushionQrCode)) return null;
        CushionInfoEntity cushionInfoEntity = findByQrCode(cushionQrCode);
        if (null == cushionInfoEntity) return null;
        log.info("onScanCodeSuccess");
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtil.copyProperties(cushionInfoEntity, cushionInfoDTO);
        cushionInfoDTO.setScannerSeq(scannerSeq);
        sseService.sendCushionMsg(cushionInfoDTO);// 推送一条缓冲垫数据到客户端
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
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        cushionInfoDTO.setWorkLine(workLine);
        cushionInfoDTO.setScannerSeq(scannerSeq);
        sseService.sendCushionMsg(cushionInfoDTO);// 推送一条缓冲垫数据到客户端
    }

    @Override
    public PageData<CushionInfoDTO> listByPage(BasePageScheme scheme) {
        Page<CushionInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<CushionInfoEntity> iPage = cushionInfoEntityMapper.listByPage(page);
        List<CushionInfoEntity> cushionInfos = iPage.getRecords();

        if (CollectionUtils.isEmpty(cushionInfos)) return null;
        return PageData.of(iPage, this::cushionInfo2DTO);
    }


    private CushionInfoDTO cushionInfo2DTO(CushionInfoEntity cushionInfo) {
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtil.copyProperties(cushionInfo, cushionInfoDTO);
        return cushionInfoDTO;
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
        if (CollectionUtils.isEmpty(cushionInfos)) return null;
        return cushionInfos.stream().map(this::cushionInfo2DTO).collect(Collectors.toList());
    }

    @Override
    public List<CushionInfoDTO> listByIds(List<Long> ids) {
        List<CushionInfoEntity> cushionInfos = cushionInfoEntityMapper.listByIds(ids);
        if (CollectionUtils.isEmpty(cushionInfos)) return null;
        return cushionInfos.stream().map(this::cushionInfo2DTO).collect(Collectors.toList());
    }

    @Override
    public boolean modifyUsedCountByQrCode(String qrCode, Integer scannerSeq, Integer count) {
        if (null == count) return false;
        CushionInfoEntity cushionInfo = new CushionInfoEntity();
        cushionInfo.setQrCode(qrCode);
        cushionInfo.setUsedCount(count);
        cushionInfo.setLastScanDate(new Date());
        cushionInfo.setScannerSeq(scannerSeq);
        return cushionInfoEntityMapper.modifyUsedCountByQrCode(cushionInfo) > 0;
    }

    @Override
    public boolean modifyOpenCountByQrCode(String qrCode, Integer openCount) {
        if (null == openCount) return false;
        CushionInfoEntity cushionInfo = new CushionInfoEntity();
        cushionInfo.setQrCode(qrCode);
        cushionInfo.setOpenCount(openCount);
        return cushionInfoEntityMapper.modifyOpenCountByQrCode(cushionInfo) > 0;
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

        EventBus.getDefault().post(new EventBusMsgPlcCmd(plcAddr.getAddr(), Constants.DEFAULT_2_PLC_VAL, workLine));

        if (Constants.PLC_ADDR_TYPE_SCAN_SUCCESS == plcAddrType)
            readOpenCountFromPLC(false, qrCode, scannerSeq, workLine);

        if (Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS == plcAddrType)
            readOpenCountFromPLC(true, qrCode, scannerSeq, workLine);
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

        if (null == plcAddrType) return;

        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerSeq(plcAddrType, scannerSeq);

        if (null == plcAddr) return;

        EventBus.getDefault().post(new EventBusMsgReadOpenCountFromPLC(qrCode, plcAddr.getAddr(), workLine));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgCushionQrCode event) {
        log.info("onMessageEvent，EventBusMsgCushionQrCode：{}", event);
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
