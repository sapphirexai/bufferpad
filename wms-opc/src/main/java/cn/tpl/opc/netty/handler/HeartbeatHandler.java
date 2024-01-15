package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.netty.Connection;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/7
 * 心跳处理器
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    /**
     * 连接信息
     */
    private final Connection mConnection;

    public HeartbeatHandler(Connection connection) {
        mConnection = connection;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        // 如果当前触发的事件是闲置事件
        if (event instanceof IdleStateEvent idleEvent) {
            // 如果当前通道触发了写闲置事件
            if (idleEvent.state() == IdleState.WRITER_IDLE) {
                // 表示当前客户端有一段时间未向服务端发送数据了，
                // 为了防止服务端关闭当前连接，手动发送一个心跳包
                String hb = Constants.SCANNER_MSG_STX + Constants.SCANNER_MSG_HEART_BEAT + Constants.SCANNER_MSG_ETX;
                ctx.channel().writeAndFlush(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(hb.getBytes(CharsetUtil.UTF_8))).duplicate());
                log.info("userEventTriggered, send heartbeat success {}, ip => {}:{}", hb, mConnection.getIp(), mConnection.getPort());
            } else {
                super.userEventTriggered(ctx, event);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive, connect success, ip => {}:{}", mConnection.getIp(), mConnection.getPort());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.warn("channelInactive, connection closed....");
        onConnectionClosed();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("exceptionCaught, connection exception => ", cause);
        ctx.close();
        onConnectionClosed();
    }

    /**
     * 连接被迫关闭时调用
     */
    private void onConnectionClosed() {
        log.info("onConnectionClosed");
        mConnection.nowDead();
    }
}

