package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.netty.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty数据处理器
 */
@Slf4j
public class MsgHandler extends ChannelInboundHandlerAdapter {
    /**
     * Ascii值匹配正则，用于替换数据中的Ascii帧头和帧尾
     */
    private static final String SCANNER_DATA_REGEX = "[\\x02-\\x03]";

    /**
     * 连接信息
     */
    private final Connection mConnection;

    public MsgHandler(Connection connection) {
        mConnection = connection;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            if (in.isReadable()) {
                String oMsg = in.toString(CharsetUtil.UTF_8);
                log.info("channelRead，收到原始数据：{}", oMsg);
                // 如果是心跳包，则不做操作
                if (oMsg.contains(Constants.SCANNER_MSG_HEART_BEAT)) {
                    onHearBeat();
                    return;
                }
                int workLine = mConnection.getWorkLine();
                if (oMsg.contains(Constants.SCANNER_MSG_NO_READ)) {
                    log.info("channelRead，扫码器未读到数据。");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(null, workLine));
                    // 扫码失败PLC报警
                    EventBus.getDefault().post(new EventBusMsgPlcCmd(Constants.PLC_DATA_ADDRESS_D6600, 1,workLine));
                    return;
                }

                // 帧头匹配就进行下一步操作
                if (oMsg.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))) {
                    log.info("channelRead，扫码数据帧头匹配，*** 开始操作 ***。");
                    // 替换帧头和帧尾
                    String fMsg = oMsg.replaceAll(SCANNER_DATA_REGEX, "");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(fMsg, mConnection.getWorkLine()));
                }
            }
        } finally {
            // 使用完须释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 收到心跳时
     */
    protected void onHearBeat() {
        log.info("onHearBeat，来自扫码器的心跳");
        mConnection.resetConnectionResetInterval();
    }

}
