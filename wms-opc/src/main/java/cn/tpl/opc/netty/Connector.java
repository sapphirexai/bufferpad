package cn.tpl.opc.netty;

import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.ISseService;
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
        try {
            // 若连接已存在就返回成功
            if (connectionExists(ip, port)) return true;
            synchronized (this) {
                // 获取锁后进行二次判断
                if (connectionExists(ip, port)) return true;

                // 设置状态监听器
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

                // 保存连接信息到列表
                connectionMgr.saveConnection(ip, port, conn);
                // 创建一个客户端）
                Bootstrap client = fastBuildClient(conn);
                // 发起连接
                ChannelFuture cf = client.connect(ip, port).sync();
                conn.nowActive(cf);
            }
        } catch (Exception e) {
            log.error("Netty连接异常", e);
            return false;
        }
        return true;
    }

    public void startReconnectService() {
        long timeExecuteSec = 120L;// 执行时间，单位：秒
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
                    // 连接状态若处于活跃则不操作
                    if (conn.isActive()) continue;

                    reconnectExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String ip = conn.getIp();
                                int port = conn.getPort();
                                log.info("当前连接已断开，尝试重连 => {}:{}...", ip, port);
                                Bootstrap client = fastBuildClient(conn);
                                ChannelFuture cf = client.connect(ip, port).sync();
                                conn.nowActive(cf);
                            } catch (Exception e) {
                                log.error("Netty连接异常", e);
                            }
                        }
                    });
                }
            }
        }, timeExecuteSec, timeExecuteSec, TimeUnit.SECONDS);

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
