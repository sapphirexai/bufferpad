package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.constant.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            if (in.isReadable()) {
                String oMsg = in.toString(CharsetUtil.UTF_8);
                log.info("channelRead，收到原始数据：{}", oMsg);
                // 如果是心跳包，则不做操作
                if (oMsg.contains(Constants.SCANNER_MSG_HEART_BEAT)) {
                    log.info("channelRead，来自扫码器心跳包，不操作。");
                    return;
                }

                if (oMsg.contains(Constants.SCANNER_MSG_NO_READ)) {
                    log.info("channelRead，扫码器未读到数据，不操作。");
                    return;
                }

                // 帧头匹配就进行下一步操作
                if (oMsg.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))) {
                    log.info("channelRead，扫码数据帧头匹配，*** 开始操作 ***。");
                    // 替换帧头和帧尾
                    String fMsg = oMsg.replaceAll(SCANNER_DATA_REGEX, "");
                    onScannerMsgReceived(fMsg);
                }
            }
        } finally {
            // 使用完须释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 扫码器消息收到时调用
     *
     * @param fMsg 最终扫码数据
     */
    protected void onScannerMsgReceived(String fMsg) {
        log.info("onScannerMsgReceived, msg: [{}]", fMsg);
    }
}
