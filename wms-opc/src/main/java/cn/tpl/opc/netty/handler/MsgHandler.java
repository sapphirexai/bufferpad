package cn.tpl.opc.netty.handler;

import cn.tpl.opc.ApplicationContextAwareImpl;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.service.IPLCAddrService;
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
     * 矽瞻扫码器匹配正则，用于替换数据中自定义的帧头和帧尾
     *
     * @see Constants#SCANNER_XZ_STX
     * @see Constants#SCANNER_XZ_ETX
     */
    private static final String XZ_SCANNER_DATA_REGEX = "\\[TPL_[S|E]TX]";

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
                log.info("channelRead, read success, data => {}", oMsg);
                // 如果是心跳包，则不做操作
                if (oMsg.contains(Constants.SCANNER_MSG_HEART_BEAT)) {
                    onHearBeat();
                    return;
                }

                int workLine = mConnection.getWorkLine();
                int installSeq = mConnection.getInstallSeq();
                String scannerHost = mConnection.getIp() + ":" + mConnection.getPort();
                String scannerName = mConnection.getName();
                String scannerPosition = mConnection.getPosition();
                if (oMsg.contains(Constants.SCANNER_MSG_NO_READ)) {
                    log.info("channelRead, no read");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(null, workLine, scannerHost, scannerName, scannerPosition, installSeq));
//                    notifyPLC(Constants.PLC_ADDR_TYPE_SCAN_FAILED, installSeq, workLine);
                    return;
                }

                // 帧头匹配就进行下一步操作
                if (oMsg.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))) {
                    logOutMatched();
                    // 替换帧头和帧尾
                    String fMsg = oMsg.replaceAll(SCANNER_DATA_REGEX, "");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(fMsg, mConnection.getWorkLine(), scannerHost, scannerName, scannerPosition, installSeq));
                }

                if (oMsg.startsWith(Constants.SCANNER_XZ_STX)) {
                    logOutMatched();
                    // 替换帧头和帧尾
                    String fMsg = oMsg.replaceAll(XZ_SCANNER_DATA_REGEX, "");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(fMsg, mConnection.getWorkLine(), scannerHost, scannerName, scannerPosition, installSeq));
                }
            }
        } finally {
            // 使用完须释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    private void logOutMatched() {
        log.info("channelRead, scanner data matched, *** START ***");
    }

    /**
     * 通知PLC
     *
     * @param plcAddrType PLC寄存器地址类型
     * @param scannerSeq  扫码器安装顺序
     * @param workLine    产线
     */
    @SuppressWarnings("all")
    private void notifyPLC(Integer plcAddrType, Integer scannerSeq, Integer workLine) {
        if (null == plcAddrType) return;

        IPLCAddrService ps = ApplicationContextAwareImpl.getPLCAddrService();
        PLCAddrEntity plcAddr = ps.findByTypeAndScannerSeq(plcAddrType, scannerSeq);
        if (null == plcAddr) return;

        EventBus.getDefault().post(new EventBusMsgPlcCmd(null, plcAddrType, plcAddr.getAddr(), Constants.DEFAULT_2_PLC_VAL, workLine));
    }

    /**
     * 收到心跳时
     */
    protected void onHearBeat() {
        log.info("onHearBeat, heartbeat from scanner");
        mConnection.resetConnectionResetInterval();
    }

}
