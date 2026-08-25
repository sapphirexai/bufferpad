package cn.tpl.opc.application.plc;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.enums.DeviceConnectionState;
import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.infrastructure.plc.PlcErrorClassifier;
import cn.tpl.opc.infrastructure.plc.PlcFailureType;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.IScanLogService;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * The single EventBus subscriber for all PLC commands. It routes each command
 * to exactly one connection and owns final logging, state changes and UI events.
 */
@Slf4j
@Component
public class PlcCommandDispatcher implements InitializingBean, DisposableBean {
    @Resource
    private ConnectionMgr connectionMgr;
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private ICushionInfoService cushionInfoService;
    @Resource
    private IOperationEventService operationEventService;
    @Resource
    private PlcErrorClassifier plcErrorClassifier;

    @Override
    public void afterPropertiesSet() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlcCommand(EventBusMsgPlcCmd event) {
        if (event == null || event.getPlcId() == null) {
            log.warn("PLC command ignored because plcId is missing, event => {}", event);
            return;
        }

        Connection connection = connectionMgr.getConnection(event.getPlcId());
        if (!isConnectionReady(connection)) {
            if (isBusinessCommand(event)) {
                addSkippedLog(event);
                publishFailure(OperationEventCode.PLC_OFFLINE, event, connection, null, "PLC连接不可用");
            }
            return;
        }

        if (isBusinessCommand(event)) {
            log.info("writing command to PLC, plcId => {}, address => {}, value => {}",
                    event.getPlcId(), event.getAddress(), event.getCmd());
        }
        PlcIoResult<Void> result = connection.write(event.getAddress(), event.getCmd());
        if (!result.isSuccess()) {
            handleWriteFailure(event, connection, result);
            return;
        }

        // A heartbeat only proves transport availability. Keep an operator-visible
        // business rejection until a real business command succeeds.
        if (isBusinessCommand(event)
                || !DeviceConnectionState.DEGRADED.name().equals(connection.getStatusCode())) {
            connection.markOnline("PLC通信正常");
        }
        if (isBusinessCommand(event)) {
            scanLogService.add(event.getQrCode(), Constants.SCAN_LOG_MSG_NOTIFY_PLC_SUCCESS + event.getAddress()
                    + Constants.SCAN_LOG_MSG_SUFFIX_NOTIFY_PLC_CMD + event.getCmd(), Constants.SCAN_LOG_TYPE_INFO);
            publishSuccess(event, connection);
            log.info("writing command to PLC succeeded, plcId => {}, address => {}", event.getPlcId(), event.getAddress());
        }

        if (event.getReadAddress() != null && !event.getReadAddress().isBlank()) {
            readOpenCount(event.getOperationId(), event.getQrCode(), event.getScannerId(),
                    event.getWorkLine(), connection, event.getReadAddress());
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onLegacyReadCommand(EventBusMsgReadOpenCountFromPLC event) {
        if (event == null || event.getPlcId() == null) return;
        Connection connection = connectionMgr.getConnection(event.getPlcId());
        if (!isConnectionReady(connection)) {
            scanLogService.add(event.getQrCode(), Constants.SCAN_LOG_MSG_READ_OPEN_COUNT_FAILED + event.getAddress(), Constants.SCAN_LOG_TYPE_ERROR);
            return;
        }
        readOpenCount(null, event.getQrCode(), null, event.getWorkLine(), connection, event.getAddress());
    }

    private void handleWriteFailure(EventBusMsgPlcCmd event, Connection connection, PlcIoResult<Void> result) {
        PlcFailureType failureType = plcErrorClassifier.classify(result.getErrorCode(), result.getMessage());
        log.error("writing command to PLC failed, plcId => {}, address => {}, errorCode => {}, error => {}",
                event.getPlcId(), event.getAddress(), result.getErrorCode(), result.getMessage());

        if (failureType == PlcFailureType.TRANSPORT) {
            connection.nowDead("PLC网络或连接异常", result.getErrorCode());
        } else {
            connection.markDegraded(result.getErrorCode(), operatorReason(result));
        }

        if (!isBusinessCommand(event)) return;
        scanLogService.add(event.getQrCode(), Constants.SCAN_LOG_MSG_NOTIFY_PLC_FAILED + event.getAddress()
                + Constants.SCAN_LOG_MSG_SUFFIX_NOTIFY_PLC_CMD + event.getCmd(), Constants.SCAN_LOG_TYPE_ERROR);
        OperationEventCode code = failureType == PlcFailureType.TRANSPORT
                ? OperationEventCode.PLC_WRITE_FAILED
                : OperationEventCode.PLC_WRITE_REJECTED;
        publishFailure(code, event, connection, result.getErrorCode(), result.getMessage());
    }

    private void readOpenCount(String operationId, String qrCode, Long scannerId, Integer workLine,
                               Connection connection, String address) {
        PlcIoResult<Short> result = connection.readInt16(address);
        if (!result.isSuccess()) {
            PlcFailureType failureType = plcErrorClassifier.classify(result.getErrorCode(), result.getMessage());
            if (failureType == PlcFailureType.TRANSPORT) {
                connection.nowDead("PLC网络或连接异常", result.getErrorCode());
            } else {
                connection.markDegraded(result.getErrorCode(), operatorReason(result));
            }
            scanLogService.add(qrCode, Constants.SCAN_LOG_MSG_READ_OPEN_COUNT_FAILED + address, Constants.SCAN_LOG_TYPE_ERROR);
            OperationEventDTO event = baseEvent(OperationEventCode.PLC_READ_FAILED, operationId,
                    qrCode, scannerId, workLine, connection, null);
            event.setAddress(address);
            event.setErrorCode(result.getErrorCode());
            event.setTechnicalDetail(result.getMessage());
            operationEventService.publish(event);
            return;
        }

        Short openCount = result.getContent();
        if (openCount == null || openCount < 0) {
            connection.markDegraded(null, "PLC开口数超出有效范围");
            OperationEventDTO event = baseEvent(OperationEventCode.PLC_READ_FAILED, operationId,
                    qrCode, scannerId, workLine, connection, null);
            event.setAddress(address);
            event.setMessage("PLC开口数必须是0到32767之间的INT值");
            event.setTechnicalDetail("PLC returned invalid Int16 open count: " + openCount);
            operationEventService.publish(event);
            return;
        }

        connection.markOnline("PLC通信正常");
        if (!cushionInfoService.modifyOpenCountByQrCode(qrCode, openCount)) {
            OperationEventDTO event = baseEvent(OperationEventCode.PLC_READ_FAILED, operationId,
                    qrCode, scannerId, workLine, connection, null);
            event.setAddress(address);
            event.setMessage("PLC已返回开口数，但系统未能更新对应缓冲垫");
            operationEventService.publish(event);
        }
    }

    private void publishSuccess(EventBusMsgPlcCmd command, Connection connection) {
        OperationEventDTO event = baseEvent(OperationEventCode.PLC_NOTIFY_SUCCEEDED, command.getOperationId(), command.getQrCode(),
                command.getScannerId(), command.getWorkLine(), connection, command.getPlcId());
        event.setAddress(command.getAddress());
        event.setMessage("PLC已成功写入 " + command.getAddress() + "，写入值 " + command.getCmd());
        operationEventService.publish(event);
    }

    private void publishFailure(OperationEventCode code, EventBusMsgPlcCmd command, Connection connection,
                                Integer errorCode, String technicalDetail) {
        OperationEventDTO event = baseEvent(code, command.getOperationId(), command.getQrCode(), command.getScannerId(),
                command.getWorkLine(), connection, command.getPlcId());
        event.setAddress(command.getAddress());
        event.setErrorCode(errorCode);
        event.setTechnicalDetail(technicalDetail);
        operationEventService.publish(event);
    }

    private OperationEventDTO baseEvent(OperationEventCode code, String operationId, String qrCode, Long scannerId,
                                        Integer workLine, Connection connection, Long configuredPlcId) {
        OperationEventDTO event = OperationEventDTO.of(code, workLine);
        event.setOperationId(operationId);
        event.setQrCode(qrCode);
        event.setScannerId(scannerId);
        DeviceInfoEntity scanner = scannerId == null ? null : deviceInfoEntityMapper.selectByPrimaryKey(scannerId);
        event.setScannerSeq(scanner == null ? null : scanner.getInstallSeq());
        if (scanner != null) {
            event.setScannerName(scanner.getName());
            event.setScannerIp(scanner.getIp());
        }

        Long plcId = connection != null && connection.getId() != null ? connection.getId() : configuredPlcId;
        DeviceInfoEntity plc = plcId == null ? null : deviceInfoEntityMapper.selectByPrimaryKey(plcId);
        event.setPlcId(plcId);
        event.setPlcName(plc == null ? (connection == null ? null : connection.getName()) : plc.getName());
        event.setPlcIp(plc == null ? null : plc.getIp());
        event.setDeviceId(plcId);
        event.setDeviceName(event.getPlcName());
        return event;
    }

    private String operatorReason(PlcIoResult<?> result) {
        if (result.getErrorCode() == 85) return "PLC禁止运行中写入";
        return "PLC拒绝本次操作";
    }

    private boolean isConnectionReady(Connection connection) {
        return connection != null && connection.isActive() && !connection.isNoPLCNet();
    }

    private boolean isBusinessCommand(EventBusMsgPlcCmd event) {
        return Constants.PLC_ADDR_TYPE_HEART_BEAT != event.getAddrType();
    }

    private void addSkippedLog(EventBusMsgPlcCmd event) {
        scanLogService.add(event.getQrCode(), Constants.SCAN_LOG_MSG_NOTIFY_PLC_SKIPPED + event.getAddress()
                + Constants.SCAN_LOG_MSG_SUFFIX_NOTIFY_PLC_CMD + event.getCmd(), Constants.SCAN_LOG_TYPE_ERROR);
    }
}
