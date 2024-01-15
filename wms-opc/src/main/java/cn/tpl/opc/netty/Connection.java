package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.tpl.opc.ApplicationContextAwareImpl;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.event.EventBusMsgReadOpenCountFromPLC;
import cn.tpl.opc.service.ICushionInfoService;
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

    private OnStatusChangeListener onStatusChangeListener;

    /**
     * Netty连接重置间隔时间
     */
    private final AtomicLong connectionResetInterval = new AtomicLong(Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC);

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService connectionCheckService = Executors.newScheduledThreadPool(1);

//    /**
//     * 连接检查任务
//     */
//    private final Runnable connectionCheckTask = () -> {
//        if (isActive()) {
//            long resetInterval = connectionResetInterval.decrementAndGet();
//            if (0 > resetInterval) nowDead();
//        }
//    };

    public Connection() {
    }

//    /**
//     * 参数初始化完毕后, 首次连接之前调用
//     */
//    public void readyToConnect() {
//        if (Params.DEVICE_TYPE_KEY_SCANNER == type)
//            connectionCheckService.scheduleAtFixedRate(connectionCheckTask, 0, 1, TimeUnit.SECONDS);
//    }

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

        if (cf.channel().isWritable()) {
            log.info("nowActive, activated, set status active");
            status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        }

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
            log.info("nowActive, 连接已活不做操作");
            return;
        }

        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        setMelsecMcNet(melsecMcNet);
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    /**
     * 改变连接状态为断开
     */
    public synchronized void nowDead() {
        if (isDead()) {
            log.info("nowDead, 连接已死不做操作");
            return;
        }

        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        if (null != channelFuture) {
            channelFuture.channel().close();
            channelFuture = null;
        }

        if (null != melsecMcNet) {
            melsecMcNet.ConnectClose();
            melsecMcNet = null;
            if (EventBus.getDefault().isRegistered(this))
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
        log.info("onMessageEvent, EventBusMsgPlcCmd: {}", event);
        if (null == melsecMcNet) return;
        if (!workLine.equals(event.getWorkLine())) return;
        String addr = event.getAddress();
        int cmd = event.getCmd();
        log.info("onMessageEvent, writing cmd to PLC =>> Address: {}, Cmd: {}", addr, cmd);
        OperateResult operateResult = melsecMcNet.Write(addr, cmd);
        if (!operateResult.IsSuccess) {
            log.error("onMessageEvent, writing cmd to PLC =>> failed, address: {}, cmd: {}", addr, cmd);
            log.error("onMessageEvent, ErrorCode: {}", operateResult.ErrorCode);
            log.error("onMessageEvent, ErrorMsg: {}", operateResult.Message);
            return;
        }
        log.info("onMessageEvent, writing cmd to PLC =>> success");
        //            nowDead();// 关闭PLC连接等待重连
//        if (isDead()) nowActive(melsecMcNet);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgReadOpenCountFromPLC event) {
        log.info("onMessageEvent, EventBusMsgReadOpenCountFromPLC: {}", event);

        if (null == melsecMcNet) return;

        if (!workLine.equals(event.getWorkLine())) return;

        String addr = event.getAddress();
        log.info("onMessageEvent, reading from PLC =>> Address: {}", addr);
        OperateResultExOne<Integer> operateResult = melsecMcNet.ReadInt32(addr);
        if (!operateResult.IsSuccess) {
            log.error("onMessageEvent, reading from PLC =>> failed, address: {}", addr);
            log.error("onMessageEvent, ErrorCode: {}", operateResult.ErrorCode);
            log.error("onMessageEvent, ErrorMsg: {}", operateResult.Message);
            return;
        }

        Integer content = operateResult.Content;
        if (null == content) {
            log.error("onMessageEvent, reading from PLC =>> openCount is null");
            return;
        }
        ICushionInfoService cs = (ICushionInfoService) ApplicationContextAwareImpl.getBean("cushionInfoService");
        boolean result = cs.modifyOpenCountByQrCode(event.getQrCode(), content);
        log.info("onMessageEvent, reading from PLC =>> success");
        if (result) {
            log.info("onMessageEvent, reading from PLC =>> modify openCount success");
            return;
        }
        log.info("onMessageEvent, reading from PLC =>> modify openCount failed");
    }
}
