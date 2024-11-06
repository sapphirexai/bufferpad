package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.hutool.core.util.ObjectUtil;
import cn.tpl.opc.ApplicationContextAwareImpl;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.util.NetUtils;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/11
 * 连接信息
 */
@Data
@Slf4j
public class Connection {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备类型
     *
     * @see Params#DEVICE_TYPE_KEY_SCANNER
     * @see Params#DEVICE_TYPE_KEY_SL_PLC
     * @see Params#DEVICE_TYPE_KEY_HC_PLC
     */
    private Integer type;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 连接状态
     *
     * @see Params#NETTY_CONNECTION_KEY_STATUS_DISCONNECTED
     * @see Params#NETTY_CONNECTION_KEY_STATUS_ACTIVE
     */
    private volatile Integer status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;

    /**
     * 设备名字
     */
    private String name;

    /**
     * 产线
     */
    private Integer workLine;

    /**
     * 安装顺序
     */
    private Integer installSeq;

    /**
     * Netty连接之后产生的I/O操作通道
     */
    private volatile ChannelFuture channelFuture;

    /**
     * PLC连接后产生的I/O操作通道
     */
    private volatile MelsecMcNet melsecMcNet;
    private volatile InovanceTcpNet inovanceTcpNet;

    private OnStatusChangeListener onStatusChangeListener;

    /**
     * Netty连接重置间隔时间
     */
    private final AtomicLong connectionResetInterval = new AtomicLong(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService connectionCheckService = Executors.newScheduledThreadPool(1);


    public Connection() {
    }


    /**
     * 判断连接是否处于活跃状态
     *
     * @return 活跃状态
     */
    public boolean isActive() {
        return Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE == status;
    }

    /**
     * 判断连接是否已断开
     *
     * @return 连接状态
     */
    public boolean isDead() {
        return Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED == status;
    }

    /**
     * 重置连接重置间隔时间
     */
    public void resetConnectionResetInterval() {
        connectionResetInterval.set(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);
    }

    /**
     * 改变连接状态为活跃
     *
     * @param cf 通道
     */
    public synchronized void nowActive(ChannelFuture cf) {
        if (isActive()) {
            log.info("nowActive, already activated, no operation next");
            return;
        }

        log.info("nowActive, activated, set status active");
        status2Active();

        resetConnectionResetInterval();
        setChannelFuture(cf);
    }

    public synchronized void nowActive(MelsecMcNet melsecMcNet, InovanceTcpNet inovanceTcpNet) {
        if (isActive()) {
            log.info("nowActive, already activated, no operation next");
            return;
        }
        status2Active();
        setMelsecMcNet(melsecMcNet);
        setInovanceTcpNet(inovanceTcpNet);
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    /**
     * 改变连接状态为断开
     */
    public synchronized void nowDead() {
        if (isDead()) {
            log.info("nowDead, already dead, no operation next");
            return;
        }

        status2Disconnected();
        if (null != channelFuture) {
            channelFuture.channel().close();
            channelFuture = null;
        }

        if (ObjectUtil.isNotNull(melsecMcNet)) {
            melsecMcNet.ConnectClose();
            melsecMcNet = null;
        }

        if (ObjectUtil.isNotNull(inovanceTcpNet)) {
            inovanceTcpNet.ConnectClose();
            inovanceTcpNet = null;
        }

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public interface OnStatusChangeListener {
        void onStatusChanged(Connection conn);
    }

    private boolean plcEventCheckNotPassed() {
        if (isNoPLCNet()) return true;
        if (ObjectUtil.isNotNull(melsecMcNet) && NetUtils.pingFailed(melsecMcNet.getIpAddress())) return true;
        return ObjectUtil.isNotNull(inovanceTcpNet) && NetUtils.pingFailed(inovanceTcpNet.getIpAddress());
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgPlcCmd event) {
        log.info("onMessageEvent, EventBusMsgPlcCmd: {}", event);
        if (isDead() && Constants.PLC_ADDR_TYPE_HEART_BEAT != event.getAddrType()) return;

        if (plcEventCheckNotPassed()) return;

        if (!workLine.equals(event.getWorkLine())) return;

        String addr = event.getAddress();
        Short cmd = event.getCmd();
        log.info("onMessageEvent, writing cmd to PLC =>> Address: {}, Cmd: {}", addr, cmd);
        if (null == cmd) return;

        OperateResult operateResult;
        if (ObjectUtil.isNull(melsecMcNet))
            operateResult = inovanceTcpNet.Write(addr, cmd);
        else
            operateResult = melsecMcNet.Write(addr, cmd);


        if (!operateResult.IsSuccess) {
            log.error("onMessageEvent, writing cmd to PLC =>> failed, address: {}, cmd: {}", addr, cmd);
            log.error("onMessageEvent, ErrorCode: {}", operateResult.ErrorCode);
            log.error("onMessageEvent, ErrorMsg: {}", operateResult.Message);
            status2Disconnected();
            return;
        }
        status2Active();
        log.info("onMessageEvent, writing cmd to PLC =>> success");
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgReadOpenCountFromPLC event) {
        log.info("onMessageEvent, EventBusMsgReadOpenCountFromPLC: {}", event);
        if (isDead()) return;

        if (plcEventCheckNotPassed()) return;

        if (!workLine.equals(event.getWorkLine())) return;

        String addr = event.getAddress();
        log.info("onMessageEvent, reading from PLC =>> Address: {}", addr);
        OperateResultExOne<Short> operateResult;
        if (ObjectUtil.isNull(melsecMcNet))
            operateResult = inovanceTcpNet.ReadInt16(addr);
        else
            operateResult = melsecMcNet.ReadInt16(addr);


        if (!operateResult.IsSuccess) {
            log.error("onMessageEvent, reading from PLC =>> failed, address: {}", addr);
            log.error("onMessageEvent, ErrorCode: {}", operateResult.ErrorCode);
            log.error("onMessageEvent, ErrorMsg: {}", operateResult.Message);
            status2Disconnected();
            return;
        }

        Short content = operateResult.Content;
        if (null == content) {
            log.error("onMessageEvent, reading from PLC =>> openCount is null");
            return;
        }
        log.info("onMessageEvent, reading from PLC =>> openCount is {}", content);

        ICushionInfoService cs = (ICushionInfoService) ApplicationContextAwareImpl.getBean("cushionInfoService");
        boolean result = cs.modifyOpenCountByQrCode(event.getQrCode(), content);
        log.info("onMessageEvent, reading from PLC =>> success");
        if (result) {
            log.info("onMessageEvent, reading from PLC =>> modify openCount success");
            return;
        }
        log.info("onMessageEvent, reading from PLC =>> modify openCount failed");
    }

    /**
     * 验证PLC网络操作类是否为空
     *
     * @return 验证结果, true: 空; false: 非空
     */
    public boolean isNoPLCNet() {
        return ObjectUtil.isNull(melsecMcNet) && ObjectUtil.isNull(inovanceTcpNet);
    }

    public void status2Disconnected() {
        if (isDead()) return;
        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        if (null != onStatusChangeListener)
            onStatusChangeListener.onStatusChanged(this);
    }

    public void status2Active() {
        if (isActive()) return;
        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        if (null != onStatusChangeListener)
            onStatusChangeListener.onStatusChanged(this);
    }

}
