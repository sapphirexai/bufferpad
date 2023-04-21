package cn.tpl.opc.netty;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.constant.Params;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     * 设备类型，0: 扫码器；1: PLC
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
    private Integer status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;

    /**
     * 设备名字
     */
    private String name;

    /**
     * Netty连接之后产生的I/O操作通道
     */
    private ChannelFuture channelFuture;

    /**
     * Netty客户端对象
     */
    private Bootstrap client;

    /**
     * Netty连接重置间隔时间
     */
    private long connectionResetInterval = Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC;

    /**
     * 定时执行器
     */
    private final ScheduledExecutorService connectionCheckService = Executors.newScheduledThreadPool(1);

    /**
     * 连接检查任务
     */
    private final Runnable connectionCheckTask = new Runnable() {
        @Override
        public void run() {
            if (isActive()) {
                connectionResetInterval--;
                log.info("connectionResetInterval：" + connectionResetInterval);
                if (0 > connectionResetInterval) nowDead();
            }
        }
    };


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
        connectionResetInterval = Constants.NETTY_CONNECTION_RESET_INTERVAL_SEC;
    }

    /**
     * 改变连接状态为活跃
     *
     * @param cf 通道
     */
    public void nowActive(ChannelFuture cf) {
        status = Params.NETTY_CONNECTION_KEY_STATUS_ACTIVE;
        setChannelFuture(cf);
        connectionCheckService.scheduleAtFixedRate(connectionCheckTask, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 改变连接状态为断开
     */
    public void nowDead() {
        status = Params.NETTY_CONNECTION_KEY_STATUS_DISCONNECTED;
        connectionCheckService.shutdown();
        channelFuture.channel().close();
        channelFuture = null;
    }
}
