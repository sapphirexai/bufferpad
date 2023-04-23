package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.ISseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
    private CushionInfoEntityMapper cushionInfoEntityMapper;
    @Resource
    private ISseService sseService;


    @Override
    public void onQrCodeReceived(String qrCode) {
        CushionInfoEntity cushionInfoEntity = findByQrCode(qrCode);
        if (null == cushionInfoEntity) {
            boolean addResult = add(qrCode);
            log.info("onQrCodeReceived，新增缓冲垫结果：[{}]", addResult);
            if (addResult) onScanCodeSuccess(qrCode);
            return;
        }
        //若当前与最后一次扫码时间相差不足一小时，则为无效扫码，不进行操作
        Date lastScanDate = cushionInfoEntity.getLastScanDate();
        long interval = System.currentTimeMillis() - lastScanDate.getTime();
        if (interval < Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS) {
            log.info("handleScannerData，无效扫码，不进行操作，当前扫码间隔：{}毫秒", interval);
            return;
        }

        int maxUseCount = cushionInfoEntity.getMaxUseCount();
        int usedCount = cushionInfoEntity.getUsedCount();

        // TODO: 2023/4/17 PLC设备报警
        if (maxUseCount <= usedCount) {
            log.info("handleScannerData，最大使用次数：{}，已使用次数：{}，已超次数：{}", maxUseCount, usedCount, usedCount - maxUseCount);
            EventBus.getDefault().post(new EventBusMsgPlcCmd<Integer>("test", 1));
        }
        // 增加当前缓冲垫1次使用次数
        usedCount++;
        boolean modifyResult = modifyUsedCountByQrCode(qrCode, usedCount);
        log.info("handleScannerData，增加缓冲垫已使用次数结果：[{}]", modifyResult);
        if (modifyResult) onScanCodeSuccess(qrCode);
    }

    /**
     * 缓冲垫扫码成功
     *
     * @param cushionQrCode 缓冲垫二维码
     */
    private void onScanCodeSuccess(String cushionQrCode) {
        if (StringUtils.isEmpty(cushionQrCode)) return;
        CushionInfoEntity cushionInfoEntity = findByQrCode(cushionQrCode);
        if (null == cushionInfoEntity) return;
        log.info("onScanCodeSuccess");
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtils.copyProperties(cushionInfoEntity, cushionInfoDTO);
        sseService.sendCushionMsg(cushionInfoDTO);// 推送一条缓冲垫数据到客户端
    }

    /**
     * 缓冲垫扫码失败
     */
    private void onScanCodeFailed() {
        log.info("onScanCodeFailed");
        sseService.sendCushionMsg(null);// 推送一条缓冲垫数据到客户端
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

    @Override
    public void afterPropertiesSet() throws Exception {
        EventBus.getDefault().register(this);
    }

    @Override
    public void destroy() throws Exception {
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgCushionQrCode event) {
        log.info("onMessageEvent，EventBusMsgCushionQrCode：{}", event);
        String qrCode = event.getQrCode();
        if (StringUtils.isEmpty(qrCode)) {
            onScanCodeFailed();
            return;
        }
        onQrCodeReceived(qrCode);
    }
}
