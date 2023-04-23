package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.entity.CushionInfoEntity;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.ISseService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.springframework.beans.BeanUtils;

import java.util.Date;

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
    private final ICushionInfoService mCushionInfoService;
    private final ISseService mSseService;

    public MsgHandler(Connection connection, ICushionInfoService cushionInfoService, ISseService sseService) {
        mConnection = connection;
        mCushionInfoService = cushionInfoService;
        mSseService = sseService;
        EventBus.getDefault().register(this);
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

                if (oMsg.contains(Constants.SCANNER_MSG_NO_READ)) {
                    log.info("channelRead，扫码器未读到数据。");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(null));
                    return;
                }

                // 帧头匹配就进行下一步操作
                if (oMsg.startsWith(String.valueOf(Constants.SCANNER_MSG_STX))) {
                    log.info("channelRead，扫码数据帧头匹配，*** 开始操作 ***。");
                    // 替换帧头和帧尾
                    String fMsg = oMsg.replaceAll(SCANNER_DATA_REGEX, "");
                    EventBus.getDefault().post(new EventBusMsgCushionQrCode(fMsg));
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

    /**
     * 缓冲垫扫码成功
     *
     * @param cushionQrCode 缓冲垫二维码
     */
    private void onScanCodeSuccess(String cushionQrCode) {
        if (StringUtils.isEmpty(cushionQrCode)) return;
        CushionInfoEntity cushionInfoEntity = mCushionInfoService.findByQrCode(cushionQrCode);
        if (null == cushionInfoEntity) return;
        log.info("onScanCodeSuccess");
        CushionInfoDTO cushionInfoDTO = new CushionInfoDTO();
        BeanUtils.copyProperties(cushionInfoEntity, cushionInfoDTO);
        mSseService.sendCushionMsg(cushionInfoDTO);// 推送一条缓冲垫数据到客户端
    }

    /**
     * 缓冲垫扫码失败
     */
    private void onScanCodeFailed() {
        log.info("onScanCodeFailed");
        mSseService.sendCushionMsg(null);// 推送一条缓冲垫数据到客户端
    }

    private void handleScannerData(String fMsg) {
        CushionInfoEntity cushionInfoEntity = mCushionInfoService.findByQrCode(fMsg);
        if (null == cushionInfoEntity) {
            boolean addResult = mCushionInfoService.add(fMsg);
            log.info("handleScannerData，新增缓冲垫结果：[{}]", addResult);
            if (addResult) onScanCodeSuccess(fMsg);
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

        // TODO: 2023/4/17 PLC设备报警
        if (maxUseCount <= usedCount) {
            log.info("handleScannerData，最大使用次数：{}，已使用次数：{}，已超次数：{}", maxUseCount, usedCount, usedCount - maxUseCount);
        }
        // 增加当前缓冲垫1次使用次数
        usedCount++;
        boolean modifyResult = mCushionInfoService.modifyUsedCountByQrCode(fMsg, usedCount);
        log.info("handleScannerData，增加缓冲垫已使用次数结果：[{}]", modifyResult);
        if (modifyResult) onScanCodeSuccess(fMsg);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(EventBusMsgPlcCmd<Integer> event) {
        log.info("onMessageEvent，PLC报警");
    }
}
