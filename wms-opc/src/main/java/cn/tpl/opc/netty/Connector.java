package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IPLCAddrService;
import cn.tpl.opc.service.ISseService;
import cn.tpl.opc.service.impl.PLCAddrServiceImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.concurrent.*;

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
    private ICushionInfoService cushionInfoService;
    @Resource
    private ISseService sseService;
    @Resource
    private IPLCAddrService plcAddrService;

    /**
     * 重连线程池
     */
//    private final ExecutorService reconnectExecutorService;

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService scheduledExecutorService;

    {
//        reconnectExecutorService = new ThreadPoolExecutor(
//                Runtime.getRuntime().availableProcessors() * 2,
//                Runtime.getRuntime().availableProcessors() * 4,
//                5,
//                TimeUnit.MINUTES,
//                new LinkedBlockingDeque<>(Runtime.getRuntime().availableProcessors() * 4)
//
//        );
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        startReconnectService();
    }

    private Bootstrap fastBuildClient(Connection connection) {
        String ip = connection.getIp();
        int port = connection.getPort();
        Bootstrap client = new Bootstrap();
        client.group(connectionMgr.getWorker())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) {
                        // 添加一个编码处理器，对数据编码为UTF-8格式
                        sc.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                        // 配置如果对应时间内未触发写事件，就会触发写闲置事件
                        sc.pipeline().addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));
                        // 添加一个入站处理器，对收到的数据进行处理
                        sc.pipeline().addLast(new MsgHandler(connection));
                        // 添加心跳处理器
                        sc.pipeline().addLast(new HeartbeatHandler(connection));
                    }
                });
        return client;
    }


    /**
     * 连接
     *
     * @param connection 连接对象
     * @return 连接结果
     */
    public boolean connect(Connection conn) {
        String ip = conn.getIp();
        int port = conn.getPort();
        synchronized (this) {
            // 获取锁后进行二次判断
            if (connectionExists(ip, port)) return reconnectExistConnection(ip, port);

            // 保存连接信息到列表
            conn.setOnStatusChangeListener(new Connection.OnStatusChangeListener() {
                @Override
                public void onStatusChanged(Connection conn) {
                    log.info("onStatusChanged，conn：{}", conn);
                    sendSseMsg(conn);
                }

                private void sendSseMsg(Connection conn) {
                    DeviceInfoDTO deviceInfo = new DeviceInfoDTO();
                    BeanUtils.copyProperties(conn, deviceInfo);
                    sseService.sendDeviceMsg(deviceInfo);
                }
            });
            connectionMgr.saveConnection(conn);
//            conn.readyToConnect();
            return doConnect(conn);
        }
    }


    /**
     * 重连已存在的连接，调用之前判断连接是否已经存在
     *
     * @return 重连结果
     * @see #doReconnect(Connection)
     */
    private boolean reconnectExistConnection(String ip, int port) {
        return doReconnect(connectionMgr.getConnection(ip, port));
    }

    /**
     * 重连设备，需要设备至少连接过一次，无论是否成功只要保存了连接信息就OK
     *
     * @see #doConnect(Connection)
     */
    private void reconnect() {
        ConcurrentHashMap<String, Connection> connections = connectionMgr.getConnections();
        if (CollectionUtils.isEmpty(connections)) {
            log.info("当前Netty连接列表为空，不进行重连操作...");
            return;
        }
        Collection<Connection> connectionsList = connections.values();
        for (Connection conn : connectionsList) {
            doReconnect(conn);
        }
    }

    /**
     * 连接设备，根据类型自动判断
     *
     * @param conn 连接信息
     * @throws InterruptedException
     */
    private boolean doConnect(Connection conn) {
        String ip = conn.getIp();
        int port = conn.getPort();
        if (isScannerConn(conn))
            return connectScanner(conn, ip, port);
        else
            return connectPLC(conn, ip, port);
    }

    /**
     * 重连设备
     *
     * @param conn 连接信息
     * @return 重连结果
     * @see #doConnect(Connection)
     */
    private boolean doReconnect(Connection conn) {
        if (null == conn) return false;
        if (conn.isActive()) return true;
        log.info("当前连接已断开，尝试重连 => {}:{}...", conn.getIp(), conn.getPort());
        return doConnect(conn);
    }

    /**
     * 连接扫码器
     *
     * @param conn 连接信息
     * @param ip   IP地址
     * @param port 端口号
     */
    private boolean connectScanner(Connection conn, String ip, int port) {
        try {
            log.info("connect，当前正在连接扫码器 =>> {}", ip + ":" + port);
            Bootstrap client = fastBuildClient(conn);// 创建一个客户端）
            ChannelFuture cf = client.connect(ip, port).sync(); // 发起连接
            conn.nowActive(cf);
            return true;
        } catch (Exception e) {
            log.error("Netty连接异常", e);
            return false;
        }
    }

    /**
     * 连接PLC
     *
     * @param conn 连接信息
     * @param ip   IP地址
     * @param port 端口号
     */
    private boolean connectPLC(Connection conn, String ip, int port) {
        log.info("connectPLC，当前正在连接PLC =>> {}", ip + ":" + port);
        MelsecMcNet melsecMcNet = new MelsecMcNet(ip, port);
        OperateResult operateResult = melsecMcNet.ConnectServer();
        if (operateResult.IsSuccess) {
            log.info("connectPLC，当前正在连接PLC =>> 连接成功");
            conn.nowActive(melsecMcNet);
            startPLCHeartbeatService(conn);
            return true;
        }
        log.error("connectPLC，当前正在连接PLC =>> 连接失败，当前连接地址===》{}:{}", ip, port);
        log.error("connectPLC，ErrorCode：{}", operateResult.ErrorCode);
        log.error("connectPLC，ErrorMsg：{}", operateResult.Message);
        return false;
    }

    private void startPLCHeartbeatService(Connection conn) {
        long timeExecuteSec = 2L;// 执行时间，单位：秒
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendPLCHeartBeat(conn);
            }
        }, timeExecuteSec, timeExecuteSec, TimeUnit.SECONDS);
    }

    private void sendPLCHeartBeat(Connection conn) {
        PLCAddrEntity plcAddr = plcAddrService.findByTypeAndScannerSeq(Constants.PLC_ADDR_TYPE_HEART_BEAT, conn.getInstallSeq());
        if (null != plcAddr) {
            log.info("sendPLCHeartBeat，正在发送PLC心跳包...");
            EventBus.getDefault().post(new EventBusMsgPlcCmd(plcAddr.getAddr(), Constants.HEARTBEAT_2_PLC_VAL, conn.getWorkLine()));
        }
    }

    public void startReconnectService() {
        long timeExecuteSec = 120L;// 执行时间，单位：秒
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                reconnect();
            }
        }, timeExecuteSec, timeExecuteSec, TimeUnit.SECONDS);
    }

    private boolean isScannerConn(Connection conn) {
        return Params.DEVICE_TYPE_KEY_SCANNER == conn.getType();
    }

    /**
     * 连接被迫关闭时调用
     *
     * @param ip   目标IP地址
     * @param port 目标端口
     */
    public void onConnectionClosed(String ip, int port) {
        Connection conn = connectionMgr.getConnection(ip, port);
        if (null == conn) return;

        conn.nowDead();
    }

    /**
     * 判断指定IP和端口的连接是否已存在
     *
     * @param ip   IP地址
     * @param port 端口号
     * @return true 已存在，false 不存在
     */
    private boolean connectionExists(String ip, int port) {
        return null != connectionMgr.getConnection(ip, port);
    }
}
