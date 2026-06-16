package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageParser;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.IScanLogService;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.util.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty连接器
 */
@Slf4j
@Component("connector")
public class Connector {
    @Resource
    private ConnectionMgr connectionMgr;
    @Resource
    private ISseService sseService;
    @Resource
    private IScanLogService scanLogService;
    @Resource
    private ICushionInfoService cushionInfoService;
    @Resource
    private IPLCAddrService plcAddrService;
    @Resource
    private DomainEventPublisher eventPublisher;
    @Resource
    private ScannerMessageParser scannerMessageParser;

    /**
     * 定时执行器
     */
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    private void init() {
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        startReconnectService();
        startPLCHeartbeatService();
    }

    private Bootstrap fastBuildClient(Connection conn) {
        Bootstrap client = new Bootstrap();
        client.group(connectionMgr.getWorker())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Constants.NETTY_CONNECT_TIMEOUT_MILLIS)
                .option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) {
                        // 添加一个编码处理器，对数据编码为UTF-8格式
                        sc.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                        // 配置如果对应时间内未触发写事件，就会触发写闲置事件
                        sc.pipeline().addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));
                        // 添加一个入站处理器，对收到的数据进行处理
                        sc.pipeline().addLast(new MsgHandler(conn, eventPublisher, scannerMessageParser));
                        // 添加心跳处理器
                        sc.pipeline().addLast(new HeartbeatHandler(conn));
                    }
                });

        return client;
    }


    /**
     * 连接
     *
     * @param conn 连接对象
     */
    public void connect(Connection conn) {
        Long id = conn.getId();
        if (connectionExists(id)) return;
        synchronized (this) {
            // 获取锁后进行二次判断
            if (connectionExists(id)) return;
            // 保存连接信息到列表
            conn.bindServices(scanLogService, cushionInfoService);
            conn.setOnStatusChangeListener(new Connection.OnStatusChangeListener() {
                @Override
                public void onStatusChanged(Connection conn) {
                    log.info("onStatusChanged，conn => {}", conn);
                    sendSseMsg(conn);
                }

                private void sendSseMsg(Connection conn) {
                    DeviceInfoDTO deviceInfo = new DeviceInfoDTO();
                    BeanUtil.copyProperties(conn, deviceInfo);
                    sseService.sendDeviceMsg(deviceInfo);
                }
            });
            connectionMgr.saveConnection(conn);
            doConnect(conn);
        }
    }


    /**
     * 重连设备，需要设备至少连接过一次，无论是否成功只要保存了连接信息就OK
     *
     * @see #doConnect(Connection)
     */
    private void reconnect() {
        ConcurrentHashMap<Long, Connection> connections = connectionMgr.getConnections();
        if (CollectionUtils.isEmpty(connections)) return;

        Collection<Connection> connectionsList = connections.values();
        for (Connection conn : connectionsList) {
            doReconnect(conn);
        }
    }

    /**
     * 连接设备，根据类型自动判断
     *
     * @param conn 连接信息
     */
    private void doConnect(Connection conn) {
        String ip = conn.getIp();
        Integer port = conn.getPort();
        if (isScannerConn(conn))
            connectScanner(conn, ip, port);
        else
            connectPLC(conn, ip, port);
    }

    /**
     * 重连设备
     *
     * @param conn 连接信息
     * @see #doConnect(Connection)
     */
    private void doReconnect(Connection conn) {
        if (null == conn) return;
        if (conn.isActive()) return;
        log.info("doReconnect, reconnecting...");
        doConnect(conn);
    }

    /**
     * 连接扫码器
     *
     * @param conn 连接信息
     * @param ip   IP地址
    F     * @param port 端口号
     */
    private void connectScanner(Connection conn, String ip, Integer port) {
        try {
            log.info("connectScanner, connecting => {}", ip + ":" + port);
            if (NetUtils.pingFailed(ip)) return;
            log.info("connectScanner, neetyClientInit");
            fastBuildClient(conn).connect(ip, port)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("connectScanner, connecting => success");
                            conn.nowActive(future);
                        }
                    });// 发起连接
            log.info("connectScanner, connecting => success status = > {}", conn.isActive());
        } catch (Exception e) {
            log.error("connect, error", e);
        }
    }


    /**
     * 连接PLC
     *
     * @param conn 连接信息
     * @param ip   IP地址
     * @param port 端口号
     */
    private void connectPLC(Connection conn, String ip, Integer port) {
        if (!conn.isNoPLCNet()) return;
        log.info("connectPLC, connecting => {}", ip + ":" + port);
        if (NetUtils.pingFailed(ip)) return;

        InovanceTcpNet inovanceTcpNet = null;
        MelsecMcNet melsecMcNet = null;
        OperateResult operateResult;
        if (Params.DEVICE_TYPE_KEY_HC_PLC == conn.getType()) {
            inovanceTcpNet = new InovanceTcpNet(ip, port, Constants.DEFAULT_STATION_HC_PLC_);
            operateResult = inovanceTcpNet.ConnectServer();
        } else {
            melsecMcNet = new MelsecMcNet(ip, port);
            operateResult = melsecMcNet.ConnectServer();
        }

        if (operateResult.IsSuccess) {
            log.info("connectPLC, connecting => success");
            conn.nowActive(melsecMcNet, inovanceTcpNet);
            return;
        }

        log.error("connectPLC, connecting => failed, ip =>{}:{}", ip, port);
        log.error("connectPLC, ErrorCode: {}", operateResult.ErrorCode);
        log.error("connectPLC, ErrorMsg: {}", operateResult.Message);
    }

    private void startPLCHeartbeatService() {
        long timeExecuteSec = 2L;// 执行时间，单位：秒
        scheduledExecutorService.scheduleAtFixedRate(
                this::sendPLCHeartBeat, timeExecuteSec, timeExecuteSec, TimeUnit.SECONDS);
    }

    private void sendPLCHeartBeat() {
        ConcurrentHashMap<Long, Connection> connections = connectionMgr.getConnections();
        if (CollectionUtils.isEmpty(connections)) return;

        Collection<Connection> connectionsList = connections.values();
        for (Connection conn : connectionsList) {
            if (!isScannerConn(conn)) {
                doSendPLCHeartBeat(conn);
            }
        }
    }

    private void doSendPLCHeartBeat(Connection conn) {
        if (conn.isDead() && conn.isNoPLCNet()) return;

        Collection<PLCAddrEntity> plcAddrs = plcAddrService.listByPlcIdAndType(conn.getId(), Constants.PLC_ADDR_TYPE_HEART_BEAT);
        if (CollectionUtils.isEmpty(plcAddrs)) return;

        for (PLCAddrEntity plcAddr : plcAddrs) {
            eventPublisher.publish(new EventBusMsgPlcCmd(null, Constants.PLC_ADDR_TYPE_HEART_BEAT, plcAddr.getPlcId(), plcAddr.getAddr(), Constants.HEARTBEAT_2_PLC_VAL, conn.getWorkLine()));
        }
    }

    public void startReconnectService() {
        long timeExecuteSec = 120L;// 执行时间，单位：秒
        scheduledExecutorService.scheduleAtFixedRate(
                this::reconnect, timeExecuteSec, timeExecuteSec, TimeUnit.SECONDS);
    }

    private boolean isScannerConn(Connection conn) {
        return Params.DEVICE_TYPE_KEY_SCANNER == conn.getType();
    }

    /**
     * 判断指定IP和端口的连接是否已存在
     *
     * @param id 设备ID
     * @return true 已存在
     */
    private boolean connectionExists(Long id) {
        return connectionMgr.connectionExists(id);
    }
}
