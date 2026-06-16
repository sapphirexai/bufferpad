package cn.tpl.opc.netty.handler;

import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.infrastructure.scanner.ScannerMessage;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageParser;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageType;
import cn.tpl.opc.netty.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty scanner data handler.
 */
@Slf4j
public class MsgHandler extends ChannelInboundHandlerAdapter {
    private final Connection connection;
    private final DomainEventPublisher eventPublisher;
    private final ScannerMessageParser scannerMessageParser;

    public MsgHandler(Connection connection, DomainEventPublisher eventPublisher, ScannerMessageParser scannerMessageParser) {
        this.connection = connection;
        this.eventPublisher = eventPublisher;
        this.scannerMessageParser = scannerMessageParser;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            if (!in.isReadable()) return;

            String rawMessage = in.toString(CharsetUtil.UTF_8);
            log.info("channelRead, read success, data => {}", rawMessage);

            ScannerMessage scannerMessage = scannerMessageParser.parse(rawMessage);
            if (scannerMessage.getType() == ScannerMessageType.HEARTBEAT) {
                onHearBeat();
                return;
            }

            if (scannerMessage.getType() == ScannerMessageType.NO_READ) {
                log.info("channelRead, no read");
                publishScanEvent(null);
                return;
            }

            if (scannerMessage.getType() == ScannerMessageType.BARCODE) {
                log.info("channelRead, scanner data matched, *** START ***");
                publishScanEvent(scannerMessage.getQrCode());
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void publishScanEvent(String qrCode) {
        eventPublisher.publish(new EventBusMsgCushionQrCode(
                qrCode,
                connection.getId(),
                connection.getWorkLine(),
                connection.getIp() + ":" + connection.getPort(),
                connection.getName(),
                connection.getPosition(),
                connection.getInstallSeq()
        ));
    }

    protected void onHearBeat() {
        log.info("onHearBeat, heartbeat from scanner");
        connection.resetConnectionResetInterval();
    }
}
