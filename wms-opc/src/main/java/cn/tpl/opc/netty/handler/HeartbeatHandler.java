package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.DeviceHealthProperties;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import java.util.concurrent.TimeUnit;

/** Application heartbeat is opt-in, after the scanner protocol has been confirmed. */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private final Connection connection;
    private final DeviceHealthProperties.ScannerHeartbeat config;
    private final int threshold;
    private boolean awaitingResponse;

    public HeartbeatHandler(Connection connection) { this(connection, null, 3); }
    public HeartbeatHandler(Connection connection, DeviceHealthProperties.ScannerHeartbeat config, int threshold) {
        this.connection = connection; this.config = config; this.threshold = threshold;
    }
    @Override public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (!(event instanceof IdleStateEvent) || ((IdleStateEvent) event).state() != IdleState.WRITER_IDLE) {
            super.userEventTriggered(ctx, event); return;
        }
        if (config == null || !config.isEnabled() || awaitingResponse || !connection.ownsChannel(ctx.channel())
                || !ctx.channel().isActive()) return;
        awaitingResponse = true;
        long responseSequence = connection.getResponseSequence().get();
        String heartbeat = Constants.SCANNER_MSG_STX + Constants.SCANNER_MSG_HEART_BEAT + Constants.SCANNER_MSG_ETX;
        connection.recordRequest("SCANNER_HEARTBEAT");
        ctx.writeAndFlush(Unpooled.copiedBuffer(heartbeat, CharsetUtil.UTF_8)).addListener(sent -> {
            if (!sent.isSuccess()) {
                awaitingResponse = false;
                connection.scannerClosed(ctx.channel(), "扫码器心跳发送失败");
                return;
            }
            ctx.executor().schedule(() -> {
                awaitingResponse = false;
                synchronized (connection) {
                    if (connection.ownsChannel(ctx.channel()) && ctx.channel().isActive()
                            && connection.getResponseSequence().get() == responseSequence) {
                        connection.recordRequestTimeout(threshold, null);
                    }
                }
            }, config.getResponseTimeoutSeconds(), TimeUnit.SECONDS);
        });
    }
    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connection.scannerClosed(ctx.channel(), "扫码器TCP连接已断开");
        super.channelInactive(ctx);
    }
    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        connection.scannerClosed(ctx.channel(), "扫码器TCP连接异常：" + cause.getMessage());
        ctx.close();
    }
}
