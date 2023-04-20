package cn.tpl.opc.netty;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty数据分发
 */
@Slf4j
@Component("msgBus")
public class MsgBus {
    @Resource
    private ConnectionMgr connectionMgr;

    public void sendMsg(String ip, int port, String msg) {
        Connection connection = connectionMgr.getConnection(ip, port);
        if (null == connection) return;
        boolean isActive = connection.isActive();
        if (isActive) {
            log.info("sendMsg, target: [{}], msg: [{}]", ip + ":" + port, msg);
            ChannelFuture cf = connectionMgr.getConnection(ip, port).getChannelFuture();
            cf.channel().writeAndFlush(msg);
        }
    }
}
