package cn.tpl.opc.netty;

import cn.tpl.opc.commons.constant.Params;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
}
