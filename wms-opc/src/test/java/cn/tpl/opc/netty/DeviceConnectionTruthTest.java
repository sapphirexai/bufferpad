package cn.tpl.opc.netty;

import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageParser;
import cn.tpl.opc.infrastructure.event.DomainEventPublisher;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceConnectionTruthTest {
    @Test public void tcpSuccessDoesNotProvePlcCommunication() {
        Connection c = new Connection();
        c.nowActive(mock(InovanceTcpNet.class));
        assertTrue(c.isActive()); // transport remains usable for heartbeat validation
        assertEquals("VERIFYING", c.getStatusCode());
        assertNull(c.getLastCommunicationAt());
        c.markOnline("PLC通信正常");
        assertEquals("ONLINE", c.getStatusCode());
        assertNotNull(c.getLastCommunicationAt());
        c.nowDead("timeout", 10000);
        c.nowActive(mock(InovanceTcpNet.class));
        assertEquals("VERIFYING", c.getStatusCode());
        assertNull(c.getLastErrorCode());
    }

    @Test public void scannerRequiresResponseAndPassiveSilenceIsNotAFault() {
        Connection c = new Connection();
        DomainEventPublisher events = mock(DomainEventPublisher.class);
        EmbeddedChannel ch = new EmbeddedChannel(new MsgHandler(c, events, new ScannerMessageParser()), new HeartbeatHandler(c));
        c.nowActive(ch.newSucceededFuture());
        assertEquals("VERIFYING", c.getStatusCode());
        ch.writeInbound(Unpooled.copiedBuffer("unrecognized", CharsetUtil.UTF_8));
        assertEquals("VERIFYING", c.getStatusCode());
        ch.writeInbound(Unpooled.copiedBuffer("\u0002HeartBeat\u0003", CharsetUtil.UTF_8));
        assertEquals("ONLINE", c.getStatusCode());
        ch.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
        assertEquals("ONLINE", c.getStatusCode());
        ch.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
        assertNull(ch.readOutbound());
        c.scannerResponse(ch);
        assertEquals("ONLINE", c.getStatusCode());
        ch.close();
        assertEquals("OFFLINE", c.getStatusCode());
        ch.finishAndReleaseAll();
    }

    @Test public void oldScannerChannelCannotInvalidateReplacement() {
        Connection c = new Connection();
        EmbeddedChannel old = new EmbeddedChannel(), replacement = new EmbeddedChannel();
        c.nowActive(old.newSucceededFuture());
        c.nowDead();
        c.nowActive(replacement.newSucceededFuture());
        c.scannerResponse(old);
        assertEquals("VERIFYING", c.getStatusCode());
        c.scannerResponse(replacement);
        c.scannerClosed(old, "late close");
        assertEquals("ONLINE", c.getStatusCode());
        replacement.finishAndReleaseAll();old.finishAndReleaseAll();
    }
}
