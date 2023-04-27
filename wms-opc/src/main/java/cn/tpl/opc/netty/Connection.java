package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/11
 * 连接信息
 */
@Data
@Slf4j
@ToString
public class Connection {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备类型
     *
     * @see Params#DEVICE_TYPE_KEY_SCANNER
     * @see Params#DEVICE_TYPE_KEY_PLC
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
     * Netty连接之后产生的I/O操作通道
     */
    private volatile ChannelFuture channelFuture;

    /**
     * PLC连接后产生的I/O操作通道
     */
    private volatile MelsecMcNet melsecMcNet;

    private OnStatusChangeListener onStatusChangeListener;

    /**
     * Netty连接重置间隔时间
     */
    private final AtomicLong connectionResetInterval = new AtomicLong(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService connectionCheckService = Executors.newScheduledThreadPool(1);

    /**
     * 连接检查任务
     */
    private final Runnable connectionCheckTask = () -> {
        if (isActive()) {
            long resetInterval = connectionResetInterval.decrementAndGet();
            log.info("connectionResetInterval：" + resetInterval);
            if (0 > resetInterval) nowDead();
        }
    };

    public Connection() {
        if (Params.DEVICE_TYPE_KEY_SCANNER == status)
            connectionCheckService.scheduleAtFixedRate(connectionCheckTask, 0, 1, TimeUnit.SECONDS);
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
            log.info("nowActive，连接已活不做操作");
            return;
        }

        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        resetConnectionResetInterval();
        setChannelFuture(cf);

        if (null != onStatusChangeListener)
            onStatusChangeListener.onStatusChanged(this);
    }

    /**
     * 改变连接状态为活跃
     *
     * @param melsecMcNet 通道
     */
    public synchronized void nowActive(MelsecMcNet melsecMcNet) {
        if (isActive()) {
            log.info("nowActive，连接已活不做操作");
            return;
        }

        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        setMelsecMcNet(melsecMcNet);
        boolean isRegistered = EventBus.getDefault().isRegistered(this);
        if (isRegistered) return;
        EventBus.getDefault().register(this);
    }

    /**
     * 改变连接状态为断开
     */
    public synchronized void nowDead() {
        if (isDead()) {
            log.info("nowDead，连接已死不做操作");
            return;
        }

        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        if (null != channelFuture) {
            channelFuture.channel().close();
            channelFuture = null;
        }

        if (null != melsecMcNet) {
            melsecMcNet = null;
            boolean isRegistered = EventBus.getDefault().isRegistered(this);
            if (isRegistered)
                EventBus.getDefault().unregister(this);
        }

        if (null != onStatusChangeListener)
            onStatusChangeListener.onStatusChanged(this);
    }

    public interface OnStatusChangeListener {
        void onStatusChanged(Connection conn);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgPlcCmd event) {
        log.info("onMessageEvent，EventBusMsgPlcCmd：{}", event);
        if (null == melsecMcNet) return;
        if (workLine != event.getWorkLine()) return;

        OperateResult result = melsecMcNet.Write(event.getAddress(), event.getCmd());
        if (!result.IsSuccess) {
            nowDead();
            return;
        }

        if (isDead()) nowActive(melsecMcNet);
    }
}
