package cn.tpl.opc.application.scan;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.tpl.opc.application.plc.PlcNotifyService;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.commons.dto.result.SseMsgDTO;
import cn.tpl.opc.domain.scan.ScanPolicy;
import cn.tpl.opc.entity.CushionDetailEntity;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.entity.OpcConfigEntity;
import cn.tpl.opc.mapper.CushionDetailEntityMapper;
import cn.tpl.opc.mapper.CushionInfoEntityMapper;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.service.IScanLogService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.ISseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class ScanApplicationService {
    @Resource
    private ScanPolicy scanPolicy;
    @Resource
    private ScanOperationLogService scanOperationLogs;
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private IOperationEventService operationEventService;
    @Resource
    private ISseService sseService;
    @Resource
    private PlcNotifyService plcNotifyService;
    @Resource
    private OpcConfigEntityMapper opcConfigEntityMapper;
    @Resource
    private CushionInfoEntityMapper cushionInfoEntityMapper;
    @Resource
    private CushionDetailEntityMapper cushionDetailEntityMapper;

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ResultDTO<CushionInfoDTO> handleScan(ScanCommand command) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            scanOperationLogs.trackTransaction(command.getOperationId());
        CushionInfoEntity cushionInfoEntity = findByQrCode(command.getQrCode());
        if (ObjectUtil.isNull(cushionInfoEntity)) return onScanNew(command);

        Date lastScanDate = cushionInfoEntity.getLastScanDate();
        if (scanPolicy.isRepeatedWithinEffectiveInterval(lastScanDate, System.currentTimeMillis())) {
            long interval = System.currentTimeMillis() - lastScanDate.getTime();
            log.info("handleScannerData, onScanIneffective, interval => {}ms", interval);
            return onScanIneffective(command, cushionInfoEntity);
        }

        Long effectiveScannerId = getEffectiveScannerId(command.getScannerId(), cushionInfoEntity);
        Integer effectiveScannerSeq = getEffectiveScannerSeq(command.getScannerId(), command.getScannerSeq(), cushionInfoEntity);
        String effectiveScannerPosition = getEffectiveScannerPosition(command.getScannerId(), command.getScannerPosition(), cushionInfoEntity);

        boolean modifyResult = modifyUsedCountByQrCode(cushionInfoEntity, effectiveScannerPosition, effectiveScannerId, effectiveScannerSeq);
        log.info("handleScannerData, modify usedCount, result => [{}]", modifyResult);
        if (modifyResult) {
            CushionInfoEntity updatedCushionInfo = findByQrCode(command.getQrCode());
            return onScanSuccess(command, effectiveScannerPosition, effectiveScannerSeq, updatedCushionInfo);
        }

        CushionInfoEntity latestCushionInfo = findByQrCode(command.getQrCode());
        if (latestCushionInfo != null) {
            return onScanIneffective(command, latestCushionInfo);
        }

        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, command.getWorkLine(), command.getScannerSeq()), Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
        publishScanEvent(OperationEventCode.SCAN_COUNT_FAILED, command, cushionInfoEntity, null);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
    }

    public void handleScanCodeFailed(ScanCommand command) {
        log.info("onScanCodeFailed");
        publishScanEvent(OperationEventCode.SCAN_NO_READ, command, null, null);
        plcNotifyService.notifyScanCodeFailed(command.getOperationId(), command.getScannerId(), command.getWorkLine());

        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        cushionInfoDTO.setWorkLine(command.getWorkLine());
        cushionInfoDTO.setScannerId(command.getScannerId());
        cushionInfoDTO.setScannerSeq(command.getScannerSeq());
        sseService.sendCushionMsg(cushionInfoDTO);
    }

    private ResultDTO<CushionInfoDTO> onScanNew(ScanCommand command) {
        boolean addResult;
        try {
            addResult = add(command.getWorkLine(), command.getScannerPosition(), command.getScannerId(), command.getScannerSeq(), command.getQrCode());
        } catch (org.springframework.dao.DuplicateKeyException concurrentScan) {
            // Another scan may commit the same new code after our initial lookup.
            CushionInfoEntity existing = findByQrCode(command.getQrCode());
            if (existing == null) throw concurrentScan;
            return onScanIneffective(command, existing);
        }
        log.info("onQrCodeReceived, new cushion result => [{}]", addResult);
        if (addResult) {
            CushionInfoEntity newCushionInfo = findByQrCode(command.getQrCode());
            addDetail(newCushionInfo);
            return completeScan(command, command.getScannerPosition(), command.getScannerSeq(), newCushionInfo);
        }
        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, null, command.getWorkLine(), command.getScannerSeq()), Constants.RESULT_MSG_CUSHION_ADD_FAILED);
        publishScanEvent(OperationEventCode.SCAN_COUNT_FAILED, command, null, null);
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_FAILED);
    }

    private ResultDTO<CushionInfoDTO> onScanIneffective(ScanCommand command, CushionInfoEntity cushionInfoEntity) {
        if (scanPolicy.isMaxReached(cushionInfoEntity)) {
            return completeScan(command, getEffectiveScannerPosition(command.getScannerId(), command.getScannerPosition(), cushionInfoEntity),
                    getEffectiveScannerSeq(command.getScannerId(), command.getScannerSeq(), cushionInfoEntity), cushionInfoEntity);
        }
        Long cushionScannerId = cushionInfoEntity.getScannerId();
        Integer effectiveScannerSeq = getEffectiveScannerSeq(command.getScannerId(), command.getScannerSeq(), cushionInfoEntity);
        String qrCode = cushionInfoEntity.getQrCode();
        sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, command.getWorkLine(), effectiveScannerSeq), Constants.RESULT_MSG_CUSHION_INVALID_SCAN);
        publishScanEvent(OperationEventCode.SCAN_REPEATED, command, cushionInfoEntity,
                "缓冲垫 " + qrCode + " 两小时内已扫描，本次未增加使用次数，当前为 " + cushionInfoEntity.getUsedCount() + " 次");
        plcNotifyService.notifyInvalidScan(command.getOperationId(), qrCode, command.getScannerId(),
                cushionScannerId, command.getWorkLine());
        return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_INVALID_SCAN);
    }

    private ResultDTO<CushionInfoDTO> onScanSuccess(ScanCommand command, String scannerPosition, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        addDetail(cushionInfoEntity);
        return completeScan(command, scannerPosition, scannerSeq, cushionInfoEntity);
    }

    private ResultDTO<CushionInfoDTO> completeScan(ScanCommand command, String scannerPosition, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        if (cushionInfoEntity == null) {
            return ResultDTO.failure(Constants.RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED);
        }

        CushionInfoDTO cushionInfoDTO = buildScanResultDTO(cushionInfoEntity, cushionInfoEntity.getScannerId(), scannerPosition, scannerSeq);
        if (scanPolicy.isMaxReached(cushionInfoEntity)) {
            logMaxReached(command, cushionInfoEntity);
            publishScanEvent(OperationEventCode.CUSHION_MAX_REACHED, command, cushionInfoEntity,
                    "缓冲垫 " + cushionInfoEntity.getQrCode() + " 当前 " + cushionInfoEntity.getUsedCount()
                            + " 次，" + (cushionInfoEntity.getUsedCount() > cushionInfoEntity.getMaxUseCount() ? "已超过寿命上限 " : "已达到寿命上限 ") + cushionInfoEntity.getMaxUseCount() + " 次");
            plcNotifyService.notifyScanMax(command.getOperationId(), cushionInfoEntity.getQrCode(),
                    command.getScannerId(), cushionInfoEntity.getScannerId(), command.getWorkLine());
            sseService.sendFailMsg(new SseMsgDTO<>(Constants.SSE_MSG_TOPIC_CUSHION_INFO, cushionInfoEntity, command.getWorkLine(), scannerSeq), Constants.RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX);
            return ResultDTO.failure(cushionInfoDTO, Constants.RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX);
        }

        publishScanEvent(OperationEventCode.SCAN_COUNTED, command, cushionInfoEntity,
                "缓冲垫 " + cushionInfoEntity.getQrCode() + " 已完成计数，当前使用 " + cushionInfoEntity.getUsedCount() + " 次");
        plcNotifyService.notifyScanSuccess(command.getOperationId(), cushionInfoEntity.getQrCode(),
                command.getScannerId(), cushionInfoEntity.getScannerId(), command.getWorkLine());
        sseService.sendCushionMsg(cushionInfoDTO);
        return ResultDTO.success(cushionInfoDTO);
    }

    private void logMaxReached(ScanCommand command, CushionInfoEntity cushionInfoEntity) {
        int maxUseCount = cushionInfoEntity.getMaxUseCount();
        int usedCount = cushionInfoEntity.getUsedCount();
        String qrCode = cushionInfoEntity.getQrCode();
        log.warn("handleScannerData, onScanMax, maxUseCount => {}, usedCount => {}", maxUseCount, usedCount);
    }

    private Long getEffectiveScannerId(Long scannerId, CushionInfoEntity cushionInfoEntity) {
        return scanPolicy.isManualScan(scannerId) && cushionInfoEntity != null ? cushionInfoEntity.getScannerId() : scannerId;
    }

    private Integer getEffectiveScannerSeq(Long scannerId, Integer scannerSeq, CushionInfoEntity cushionInfoEntity) {
        return scanPolicy.isManualScan(scannerId) && cushionInfoEntity != null && cushionInfoEntity.getScannerSeq() != null ? cushionInfoEntity.getScannerSeq() : scannerSeq;
    }

    private String getEffectiveScannerPosition(Long scannerId, String scannerPosition, CushionInfoEntity cushionInfoEntity) {
        if (scanPolicy.isManualScan(scannerId) && cushionInfoEntity != null && StringUtils.isNotBlank(cushionInfoEntity.getScannerPosition())) {
            return cushionInfoEntity.getScannerPosition();
        }
        return scannerPosition;
    }

    private CushionInfoDTO buildScanResultDTO(CushionInfoEntity cushionInfoEntity, Long scannerId, String scannerPosition, Integer scannerSeq) {
        if (cushionInfoEntity == null) return null;
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtil.copyProperties(cushionInfoEntity, cushionInfoDTO);
        cushionInfoDTO.setScannerPosition(scannerPosition);
        cushionInfoDTO.setScannerId(scannerId);
        cushionInfoDTO.setScannerSeq(scannerSeq);
        return cushionInfoDTO;
    }

    private boolean add(Integer workLine, String scannerPosition, Long scannerId, Integer scannerSeq, String qrCode) {
        if (StringUtils.isEmpty(qrCode)) return false;

        OpcConfigEntity opcConfig = opcConfigEntityMapper.selectByPrimaryKey(Constants.OPC_CONFIG_ID);
        CushionInfoEntity cushionInfoEntity = new CushionInfoEntity();
        cushionInfoEntity.setWorkLine(workLine);
        cushionInfoEntity.setQrCode(qrCode);
        if (opcConfig != null) {
            cushionInfoEntity.setMaxUseCount(opcConfig.getCushionMaxUseCount());
        } else {
            cushionInfoEntity.setMaxUseCount(Constants.CUSHION_DEFAULT_MAX_USE_CONT);
        }
        cushionInfoEntity.setUsedCount(Constants.CUSHION_ADD_DEFAULT_USED_COUNT);
        cushionInfoEntity.setLastScanDate(new Date());
        cushionInfoEntity.setScannerId(scannerId);
        cushionInfoEntity.setScannerSeq(scannerSeq);
        cushionInfoEntity.setScannerPosition(scannerPosition);
        return cushionInfoEntityMapper.insertSelective(cushionInfoEntity) > 0;
    }

    private CushionInfoEntity findByQrCode(String qrCode) {
        return cushionInfoEntityMapper.findByQrCode(qrCode);
    }

    private boolean modifyUsedCountByQrCode(CushionInfoEntity cushionInfoEntity, String scannerPosition, Long scannerId, Integer scannerSeq) {
        Date scanDate = new Date();
        Date lastScanDateBefore = new Date(scanDate.getTime() - Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS);
        cushionInfoEntity.setLastScanDate(scanDate);
        cushionInfoEntity.setScannerPosition(scannerPosition);
        cushionInfoEntity.setScannerId(scannerId);
        cushionInfoEntity.setScannerSeq(scannerSeq);
        return cushionInfoEntityMapper.modifyUsedCountByQrCode(cushionInfoEntity, lastScanDateBefore) > 0;
    }

    private void addDetail(CushionInfoEntity cushionInfo) {
        CushionDetailEntity cushionDetail = new CushionDetailEntity();
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreProperties("id", "modifiedDate");
        BeanUtil.copyProperties(cushionInfo, cushionDetail, copyOptions);
        cushionDetail.setCreatedDate(new Date());
        cushionDetailEntityMapper.insertSelective(cushionDetail);
    }

    private void publishScanEvent(OperationEventCode code, ScanCommand command, CushionInfoEntity cushionInfo, String message) {
        OperationEventDTO event = OperationEventDTO.of(code, command.getWorkLine());
        event.setOperationId(command.getOperationId());
        event.setScannerId(command.getScannerId());
        event.setScannerSeq(command.getScannerSeq());
        event.setScannerName(command.getScannerName());
        event.setScannerIp(command.getScannerHost());
        if (cushionInfo != null) { event.setUsedCount(cushionInfo.getUsedCount()); event.setMaxUseCount(cushionInfo.getMaxUseCount()); }
        event.setDeviceId(command.getScannerId());
        event.setDeviceName(command.getScannerName());
        event.setQrCode(cushionInfo == null ? command.getQrCode() : cushionInfo.getQrCode());
        if (message != null) event.setMessage(message);
        operationEventService.publish(event);
    }
}
