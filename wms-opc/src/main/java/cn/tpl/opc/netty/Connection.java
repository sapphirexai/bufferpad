package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Core.Net.NetworkBase.NetworkDeviceBase;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.enums.DeviceConnectionState;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime connection state. PLC commands are routed by PlcCommandDispatcher;
 * this object only owns transport resources and exposes testable I/O methods.
 */
@Data
@Slf4j
public class Connection {
    private Long id;
    private Integer type;
    private String ip;
    private Integer port;
    private volatile Integer status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
    private volatile String statusCode = DeviceConnectionState.OFFLINE.name();
    private volatile String statusReason = "尚未连接";
    private volatile Date statusChangedAt = new Date();
    private volatile Date lastCommunicationAt;
    private volatile Integer lastErrorCode;
    private String name;
    private String position;
    private Integer workLine;
    private Integer installSeq;
    private volatile ChannelFuture channelFuture;
    private volatile NetworkDeviceBase plcClient;
    private OnStatusChangeListener onStatusChangeListener;
    private final AtomicLong connectionResetInterval = new AtomicLong(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    public boolean isActive() {
        return Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE == status;
    }

    public boolean isDead() {
        return Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED == status;
    }

    public boolean tryBeginConnect() {
        return connecting.compareAndSet(false, true);
    }

    public void endConnect() {
        connecting.set(false);
    }

    public void resetConnectionResetInterval() {
        connectionResetInterval.set(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);
    }

    public synchronized void markConnecting() {
        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        transition(DeviceConnectionState.CONNECTING, "正在连接", null, false);
    }

    public synchronized void nowActive(ChannelFuture future) {
        this.channelFuture = future;
        resetConnectionResetInterval();
        markOnline("连接正常");
    }

    public synchronized void nowActive(NetworkDeviceBase client) {
        this.plcClient = client;
        resetConnectionResetInterval();
        markOnline("PLC通信正常");
    }

    public synchronized void markOnline(String reason) {
        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        lastCommunicationAt = new Date();
        transition(DeviceConnectionState.ONLINE, reason, null, true);
    }

    public synchronized void markDegraded(Integer errorCode, String reason) {
        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        transition(DeviceConnectionState.DEGRADED, reason, errorCode, false);
    }

    public synchronized void nowDead() {
        nowDead("连接已断开", null);
    }

    public synchronized void nowDead(String reason, Integer errorCode) {
        closeResources();
        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        transition(DeviceConnectionState.OFFLINE, reason == null ? "连接已断开" : reason, errorCode, false);
    }

    public PlcIoResult<Void> write(String address, Short command) {
        if (command == null) return PlcIoResult.failure(-1, "PLC写入值为空");
        if (isNoPLCNet()) return PlcIoResult.failure(10000, "PLC连接不可用");
        try {
            OperateResult result = plcClient.Write(address, command);
            return result.IsSuccess
                    ? PlcIoResult.success(null)
                    : PlcIoResult.failure(result.ErrorCode, result.Message);
        } catch (Exception e) {
            return PlcIoResult.failure(10000, e.getMessage());
        }
    }

    public PlcIoResult<Short> readInt16(String address) {
        if (isNoPLCNet()) return PlcIoResult.failure(10000, "PLC连接不可用");
        try {
            OperateResultExOne<Short> result = plcClient.ReadInt16(address);
            return result.IsSuccess
                    ? PlcIoResult.success(result.Content)
                    : PlcIoResult.failure(result.ErrorCode, result.Message);
        } catch (Exception e) {
            return PlcIoResult.failure(10000, e.getMessage());
        }
    }

    public boolean isNoPLCNet() {
        return plcClient == null;
    }

    public void status2Disconnected() {
        nowDead();
    }

    public void status2Active() {
        markOnline("通信恢复正常");
    }

    private void transition(DeviceConnectionState newState, String reason, Integer errorCode, boolean clearError) {
        String newCode = newState.name();
        boolean changed = !newCode.equals(statusCode)
                || !java.util.Objects.equals(reason, statusReason)
                || !java.util.Objects.equals(errorCode, lastErrorCode);
        statusCode = newCode;
        statusReason = reason;
        if (clearError) {
            lastErrorCode = null;
        } else if (errorCode != null) {
            lastErrorCode = errorCode;
        }
        if (!changed) return;
        statusChangedAt = new Date();
        if (onStatusChangeListener != null) onStatusChangeListener.onStatusChanged(this);
    }

    private void closeResources() {
        if (channelFuture != null) {
            channelFuture.channel().close();
            channelFuture = null;
        }
        NetworkDeviceBase client = plcClient;
        plcClient = null;
        if (client != null) {
            try {
                client.ConnectClose();
            } catch (Exception e) {
                log.warn("close PLC client failed, deviceId => {}", id, e);
            }
        }
    }

    public interface OnStatusChangeListener {
        void onStatusChanged(Connection connection);
    }
}
