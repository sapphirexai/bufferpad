package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.PlcAddrTypeEnum;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IScanLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PlcNotifyService {
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;
    @Resource
    private IPLCAddrService plcAddrService;
    @Resource
    private ConnectionMgr connectionMgr;
    @Resource
    private DomainEventPublisher eventPublisher;

    public void notifyScanSuccess(String qrCode, Long sourceScannerId, Long effectiveScannerId, Integer workLine) {
        if (isManualScan(sourceScannerId)) {
            if (effectiveScannerId == null) {
                notifyAllScanners(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, workLine);
            } else {
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS, effectiveScannerId, workLine);
            }
            return;
        }
        notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_SUCCESS, sourceScannerId, workLine);
    }

    public void notifyInvalidScan(String qrCode, Long sourceScannerId, Long effectiveScannerId, Integer workLine) {
        notifyScanSuccess(qrCode, sourceScannerId, effectiveScannerId, workLine);
    }

    public void notifyScanMax(String qrCode, Long sourceScannerId, Long cushionScannerId, Integer workLine) {
        if (isManualScan(sourceScannerId)) {
            if (cushionScannerId == null) {
                notifyAllScanners(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM, workLine);
            } else {
                notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM, cushionScannerId, workLine);
            }
            return;
        }
        notifyPLC(qrCode, Constants.PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM, sourceScannerId, workLine);
    }

    public void notifyScanCodeFailed(Long scannerId, Integer workLine) {
        notifyPLC(null, Constants.PLC_ADDR_TYPE_SCAN_FAILED, scannerId, workLine);
    }

    private void notifyAllScanners(String qrCode, int plcAddrType, Integer workLine) {
        List<DeviceInfoEntity> deviceInfoEntities = deviceInfoEntityMapper.listDeviceInfoByType(0);
        if (CollectionUtils.isEmpty(deviceInfoEntities)) return;

        for (DeviceInfoEntity deviceInfo : deviceInfoEntities) {
            if (!isTargetWorkLine(workLine, deviceInfo.getWorkLine())) continue;
            Long scannerId = deviceInfo.getId();
            if (scannerId == null) continue;
            notifyPLC(qrCode, plcAddrType, scannerId, workLine);
        }
    }

    private boolean isTargetWorkLine(Integer targetWorkLine, Integer deviceWorkLine) {
        return targetWorkLine == null
                || Constants.WORK_LINE_ALL == targetWorkLine
                || targetWorkLine.equals(deviceWorkLine);
    }

    private boolean isManualScan(Long scannerId) {
        return scannerId == null;
    }

    private void notifyPLC(String qrCode, Integer plcAddrType, Long scannerId, Integer workLine) {
        if (plcAddrType == null || scannerId == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            return;
        }

        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerId(plcAddrType, scannerId);
        if (plcAddr == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            return;
        }

        if (isPlcConnectionUnavailable(plcAddr.getPlcId())) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_NOTIFY_PLC_SKIPPED + plcAddr.getAddr() + Constants.SCAN_LOG_MSG_SUFFIX_NOTIFY_PLC_CMD + Constants.DEFAULT_2_PLC_VAL, Constants.SCAN_LOG_TYPE_ERROR);
        }

        Integer eventWorkLine = resolveEventWorkLine(workLine, scannerId);
        eventPublisher.publish(new EventBusMsgPlcCmd(qrCode, plcAddrType, plcAddr.getPlcId(), plcAddr.getAddr(), Constants.DEFAULT_2_PLC_VAL, eventWorkLine));

        if (Constants.PLC_ADDR_TYPE_SCAN_SUCCESS == plcAddrType) {
            readOpenCountFromPLC(false, qrCode, scannerId, eventWorkLine);
        }

        if (Constants.PLC_ADDR_TYPE_RE_SCAN_SUCCESS == plcAddrType) {
            readOpenCountFromPLC(true, qrCode, scannerId, eventWorkLine);
        }
    }

    private boolean isPlcConnectionUnavailable(Long plcId) {
        if (plcId == null) return true;
        Connection connection = connectionMgr.getConnection(plcId);
        return connection == null || connection.isDead() || connection.isNoPLCNet();
    }

    private void readOpenCountFromPLC(boolean isReScan, String qrCode, Long scannerId, Integer workLine) {
        if (scannerId == null) return;

        Integer plcAddrType = getOpenCountPlcAddrType(isReScan);
        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerId(plcAddrType, scannerId);
        if (plcAddr == null) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_PLC_ADDR_NOT_CONFIGURED + PlcAddrTypeEnum.labelOf(plcAddrType), Constants.SCAN_LOG_TYPE_ERROR);
            return;
        }

        if (isPlcConnectionUnavailable(plcAddr.getPlcId())) {
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_READ_OPEN_COUNT_FAILED + plcAddr.getAddr(), Constants.SCAN_LOG_TYPE_ERROR);
        }

        eventPublisher.publish(new EventBusMsgReadOpenCountFromPLC(qrCode, plcAddr.getPlcId(), plcAddr.getAddr(), workLine));
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
}
