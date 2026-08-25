package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.PlcAddrTypeEnum;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IScanLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PlcNotifyService {
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private IOperationEventService operationEventService;
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;
    @Resource
    private IPLCAddrService plcAddrService;
    @Resource
    private ConnectionMgr connectionMgr;
    @Resource
    private DomainEventPublisher eventPublisher;

    public void notifyScanSuccess(String qrCode, Long sourceScannerId, Long effectiveScannerId, Integer workLine) {
        notifyScanSuccess(null, qrCode, sourceScannerId, effectiveScannerId, workLine);
    }

    public void notifyScanSuccess(String operationId, String qrCode, Long sourceScannerId,
                                  Long effectiveScannerId, Integer workLine) {
        if (isManualScan(sourceScannerId)) {
            Long targetScannerId = resolveManualScannerId(effectiveScannerId, workLine);
            notifyPLC(operationId, qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, targetScannerId, workLine);
            return;
        }
        notifyPLC(operationId, qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, sourceScannerId, workLine);
    }

    public void notifyInvalidScan(String qrCode, Long sourceScannerId, Long effectiveScannerId, Integer workLine) {
        notifyInvalidScan(null, qrCode, sourceScannerId, effectiveScannerId, workLine);
    }

    public void notifyInvalidScan(String operationId, String qrCode, Long sourceScannerId,
                                  Long effectiveScannerId, Integer workLine) {
        notifyScanSuccess(operationId, qrCode, sourceScannerId, effectiveScannerId, workLine);
    }

    public void notifyScanMax(String qrCode, Long sourceScannerId, Long cushionScannerId, Integer workLine) {
        notifyScanMax(null, qrCode, sourceScannerId, cushionScannerId, workLine);
    }

    public void notifyScanMax(String operationId, String qrCode, Long sourceScannerId,
                              Long cushionScannerId, Integer workLine) {
        if (isManualScan(sourceScannerId)) {
            Long targetScannerId = resolveManualScannerId(cushionScannerId, workLine);
            notifyPLC(operationId, qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM,
                    targetScannerId, workLine);
            return;
        }
        notifyPLC(operationId, qrCode, Constants.PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM, sourceScannerId, workLine);
    }

    public void notifyScanCodeFailed(Long scannerId, Integer workLine) {
        notifyScanCodeFailed(null, scannerId, workLine);
    }

    public void notifyScanCodeFailed(String operationId, Long scannerId, Integer workLine) {
        notifyPLC(operationId, null, Constants.PLC_ADDR_TYPE_SCAN_FAILED, scannerId, workLine);
    }

    private boolean isManualScan(Long scannerId) {
        return scannerId == null;
    }

    private Long resolveManualScannerId(Long effectiveScannerId, Integer workLine) {
        if (effectiveScannerId != null) return effectiveScannerId;

        List<DeviceInfoEntity> scanners = deviceInfoEntityMapper.listDeviceInfoByType(0);
        Long resolvedId = null;
        if (scanners == null) return null;
        for (DeviceInfoEntity scanner : scanners) {
            if (scanner == null || scanner.getId() == null || !isTargetWorkLine(workLine, scanner.getWorkLine())) continue;
            if (resolvedId != null && !resolvedId.equals(scanner.getId())) return null;
            resolvedId = scanner.getId();
        }
        return resolvedId;
    }

    private boolean isTargetWorkLine(Integer targetWorkLine, Integer deviceWorkLine) {
        return targetWorkLine == null
                || Constants.WORK_LINE_ALL == targetWorkLine
                || targetWorkLine.equals(deviceWorkLine);
    }

    private void notifyPLC(String operationId, String qrCode, Integer plcAddrType, Long scannerId,
                           Integer workLine) {
        if (scannerId == null) {
            scanLogService.add(qrCode, "手动扫码未关联具体扫码器，未发送PLC指令", Constants.SCAN_LOG_TYPE_ERROR);
            OperationEventDTO event = OperationEventDTO.of(OperationEventCode.PLC_TARGET_NOT_RESOLVED, workLine);
            event.setOperationId(operationId);
            event.setQrCode(qrCode);
            operationEventService.publish(event);
            return;
        }
        if (plcAddrType == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            publishAddressNotConfigured(operationId, qrCode, plcAddrType, scannerId, workLine);
            return;
        }

        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerId(plcAddrType, scannerId);
        if (plcAddr == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            publishAddressNotConfigured(operationId, qrCode, plcAddrType, scannerId, workLine);
            return;
        }

        if (isPlcConnectionUnavailable(plcAddr.getPlcId())) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_NOTIFY_PLC_SKIPPED + plcAddr.getAddr() + Constants.SCAN_LOG_MSG_SUFFIX_NOTIFY_PLC_CMD + Constants.DEFAULT_2_PLC_VAL, Constants.SCAN_LOG_TYPE_ERROR);
            publishPlcOffline(operationId, qrCode, scannerId, plcAddr, workLine);
            return;
        }

        Integer eventWorkLine = resolveEventWorkLine(workLine, scannerId);
        String readAddress = resolveOpenCountAddress(operationId, plcAddrType, qrCode, scannerId, eventWorkLine, plcAddr);
        eventPublisher.publish(new EventBusMsgPlcCmd(operationId, qrCode, plcAddrType, plcAddr.getPlcId(), plcAddr.getAddr(),
                Constants.DEFAULT_2_PLC_VAL, eventWorkLine, scannerId, readAddress));
    }

    private boolean isPlcConnectionUnavailable(Long plcId) {
        if (plcId == null) return true;
        Connection connection = connectionMgr.getConnection(plcId);
        return connection == null || connection.isDead() || connection.isNoPLCNet();
    }

    private String resolveOpenCountAddress(String operationId, Integer writeType, String qrCode,
                                           Long scannerId, Integer workLine, PLCAddrEntity writePlcAddr) {
        boolean scanSuccess = Constants.PLC_ADDR_TYPE_SCAN_SUCCESS == writeType;
        boolean reScanSuccess = Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS == writeType;
        if ((!scanSuccess && !reScanSuccess) || scannerId == null) return null;

        Integer plcAddrType = getOpenCountPlcAddrType(reScanSuccess);
        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerId(plcAddrType, scannerId);
        if (plcAddr == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            OperationEventDTO event = baseEvent(OperationEventCode.PLC_READ_ADDRESS_NOT_CONFIGURED,
                    operationId, qrCode, scannerId, workLine);
            DeviceInfoEntity plc = writePlcAddr == null ? null
                    : deviceInfoEntityMapper.selectByPrimaryKey(writePlcAddr.getPlcId());
            if (writePlcAddr != null) applyPlcIdentity(event, writePlcAddr.getPlcId(), plc);
            event.setMessage("缓冲垫已计数，但未配置“" + PlcAddrTypeEnum.labelOf(plcAddrType) + "”地址");
            operationEventService.publish(event);
            return null;
        }
        return plcAddr.getAddr();
    }

    private Integer getOpenCountPlcAddrType(boolean isReScan) {
        return isReScan ? Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT : Constants.PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT;
    }

    private Integer resolveEventWorkLine(Integer workLine, Long scannerId) {
        if (workLine != null && Constants.WORK_LINE_ALL != workLine) {
            return workLine;
        }

        if (scannerId == null) {
            return workLine;
        }

        DeviceInfoEntity scanner = deviceInfoEntityMapper.selectByPrimaryKey(scannerId);
        return scanner == null || scanner.getWorkLine() == null ? workLine : scanner.getWorkLine();
    }

    private void publishAddressNotConfigured(String operationId, String qrCode, Integer plcAddrType,
                                             Long scannerId, Integer workLine) {
        OperationEventDTO event = baseEvent(OperationEventCode.PLC_ADDRESS_NOT_CONFIGURED,
                operationId, qrCode, scannerId, workLine);
        event.setMessage("未配置“" + PlcAddrTypeEnum.labelOf(plcAddrType) + "”对应的PLC地址，缓冲垫计数不受影响");
        operationEventService.publish(event);
    }

    private void publishPlcOffline(String operationId, String qrCode, Long scannerId,
                                   PLCAddrEntity plcAddr, Integer workLine) {
        OperationEventDTO event = baseEvent(OperationEventCode.PLC_OFFLINE, operationId, qrCode, scannerId, workLine);
        DeviceInfoEntity plc = deviceInfoEntityMapper.selectByPrimaryKey(plcAddr.getPlcId());
        event.setDeviceId(plcAddr.getPlcId());
        event.setDeviceName(plc == null ? null : plc.getName());
        applyPlcIdentity(event, plcAddr.getPlcId(), plc);
        event.setAddress(plcAddr.getAddr());
        operationEventService.publish(event);
    }

    private OperationEventDTO baseEvent(OperationEventCode code, String operationId, String qrCode,
                                        Long scannerId, Integer workLine) {
        Integer eventWorkLine = resolveEventWorkLine(workLine, scannerId);
        OperationEventDTO event = OperationEventDTO.of(code, eventWorkLine);
        event.setOperationId(operationId);
        event.setQrCode(qrCode);
        event.setScannerId(scannerId);
        DeviceInfoEntity scanner = scannerId == null ? null : deviceInfoEntityMapper.selectByPrimaryKey(scannerId);
        event.setScannerSeq(scanner == null ? null : scanner.getInstallSeq());
        applyScannerIdentity(event, scanner);
        return event;
    }

    private void applyScannerIdentity(OperationEventDTO event, DeviceInfoEntity scanner) {
        if (scanner == null) return;
        event.setScannerName(scanner.getName());
        event.setScannerIp(scanner.getIp());
    }

    private void applyPlcIdentity(OperationEventDTO event, Long plcId, DeviceInfoEntity plc) {
        event.setPlcId(plcId);
        if (plc == null) return;
        event.setPlcName(plc.getName());
        event.setPlcIp(plc.getIp());
    }
}
