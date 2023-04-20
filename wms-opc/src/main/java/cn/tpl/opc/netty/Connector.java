package cn.tpl.opc.netty;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.service.ICushionInfoService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
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

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
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

    /**
     * 重连线程池
     */
    private final ExecutorService reconnectExecutorService;

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService scheduledExecutorService;


    {
        reconnectExecutorService = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2,
                Runtime.getRuntime().availableProcessors() * 4,
                5,
                TimeUnit.MINUTES,
                new LinkedBlockingDeque<>(Runtime.getRuntime().availableProcessors() * 4)

        );
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        startReconnectService();
    }

    private Bootstrap fastBuildClient(String ip, int port) {
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
                        sc.pipeline().addLast(new MsgHandler() {
                            @Override
                            protected void onScannerMsgReceived(String fMsg) {
                                super.onScannerMsgReceived(fMsg);
                                handleScannerData(fMsg);
                            }
                        });
                        // 添加心跳处理器
                        sc.pipeline().addLast(new HeartbeatHandler(ip, port) {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                log.warn("channelInactive, 服务端主动关闭了连接....");
                                super.channelInactive(ctx);
                                onConnectionClosed(ip, port);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                log.error("exceptionCaught, Netty连接中异常捕获：", cause);
                                super.exceptionCaught(ctx, cause);
                                onConnectionClosed(ip, port);
                            }
                        });
                    }
                });
        return client;
    }

    private void handleScannerData(String fMsg) {
        CushionInfoEntity cushionInfoEntity = cushionInfoService.findByQrCode(fMsg);
        if (null == cushionInfoEntity) {
            boolean addResult = cushionInfoService.add(fMsg);
            log.info("handleScannerData，新增缓冲垫结果：[{}]", addResult);
            return;
        }

        //若当前与最后一次扫码时间相差不足一小时，则为无效扫码，不进行操作
        Date lastScanDate = cushionInfoEntity.getLastScanDate();
        long interval = System.currentTimeMillis() - lastScanDate.getTime();
        if (interval < Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS) {
            log.info("handleScannerData，无效扫码，不进行操作，当前扫码间隔：{}毫秒", interval);
            return;
        }

        int maxUseCount = cushionInfoEntity.getMaxUseCount();
        int usedCount = cushionInfoEntity.getUsedCount();

        if (maxUseCount <= usedCount) {
            // TODO: 2023/4/17 设备报警
            log.info("handleScannerData，最大使用次数：{}，已使用次数：{}，已超次数：{}", maxUseCount, usedCount, usedCount - maxUseCount);
        }
        // 增加当前缓冲垫1次使用次数
        boolean modifyResult = cushionInfoService.modifyUsedCountByQrCode(fMsg, usedCount + 1);
        log.info("handleScannerData，增加缓冲垫已使用次数结果：[{}]", modifyResult);
    }

    /**
     * 连接
     *
     * @param ip   目标IP地址
     * @param port 目标端口
     * @return 连接结果
     */
    public boolean connect(String ip, int port) {
        // 创建一个客户端）
        Bootstrap client = fastBuildClient(ip, port);
        // 与指定的地址建立连接
        try {
            Connection cConn = connectionMgr.getConnection(ip, port);
            if (null != cConn && cConn.isActive()) return true;

            ChannelFuture cf = client.connect(ip, port).sync();
            Connection nConn = new Connection(ip, port, Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE, cf);

            // 保存连接信息到列表
            connectionMgr.saveConnection(ip, port, nConn);
        } catch (Exception e) {
            log.error("Netty连接异常", e);
            return false;
        }
        return true;
    }

    public void startReconnectService() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                ConcurrentHashMap<String, Connection> connections = connectionMgr.getConnections();
                if (CollectionUtils.isEmpty(connections)) {
                    log.info("当前Netty连接列表为空，不进行重连操作...");
                    return;
                }
                Collection<Connection> connectionsList = connections.values();
                for (Connection conn : connectionsList) {
                    if (null == conn) continue;
                    // 判断连接是否断开
                    if (!conn.isDead()) continue;

                    reconnectExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String ip = conn.getIp();
                                int port = conn.getPort();
                                log.info("当前连接已断开，尝试重连 => {}:{}...", ip, port);
                                Bootstrap client = fastBuildClient(ip, port);
                                ChannelFuture cf = client.connect(ip, port).sync();
                                conn.setChannelFuture(cf);
                            } catch (Exception e) {
                                log.error("Netty连接异常", e);
                            }
                        }
                    });
                }
            }
        }, 5, 5, TimeUnit.MINUTES);

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

        conn.setStatus(Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED);
    }


    /**
     * 断开连接
     *
     * @param ip   目标IP地址
     * @param port 目标端口
     */
    public void disconnect(String ip, int port) {
        Connection conn = connectionMgr.getConnection(ip, port);
        if (null == conn) return;

        conn.getChannelFuture().channel().close();
        conn.setStatus(Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED);
    }
}
