package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import cn.tpl.opc.application.plc.PlcCommandDispatcher;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.event.EventBusMsgPlcCmd;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceHealthConcurrencyTest {
    @Test public void optionalProbeWaitsForExistingPlcCommandWithoutOverlap() throws Exception {
        Connection c=new Connection();c.setId(1L);c.setType(2);InovanceTcpNet client=mock(InovanceTcpNet.class);c.nowActive(client);
        CountDownLatch writing=new CountDownLatch(1), release=new CountDownLatch(1), probing=new CountDownLatch(1);
        when(client.Write("MW10010",(short)0)).thenAnswer(call->{writing.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));return OperateResult.CreateSuccessResult();});
        when(client.ReadInt16("MW10000")).thenReturn(OperateResultExOne.CreateSuccessResult((short)42));
        ConnectionMgr mgr=mock(ConnectionMgr.class);when(mgr.getConnection(1L)).thenReturn(c);
        PlcCommandDispatcher dispatcher=new PlcCommandDispatcher();ReflectionTestUtils.setField(dispatcher,"connectionMgr",mgr);
        DeviceHealthProperties.PlcProbe cfg=new DeviceHealthProperties.PlcProbe();cfg.setEnabled(true);cfg.setAddress("MW10000");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> write=pool.submit(()->dispatcher.onPlcCommand(new EventBusMsgPlcCmd(null,Constants.PLC_ADDR_TYPE_HEART_BEAT,1L,"MW10010",(short)0,1)));
            assertTrue(writing.await(2,TimeUnit.SECONDS));
            Future<?> read=pool.submit(()->{probing.countDown();new PlcHealthMonitor().probe(c,cfg,3);});
            assertTrue(probing.await(2,TimeUnit.SECONDS));Thread.sleep(100);
            assertFalse(read.isDone());verify(client,never()).ReadInt16(anyString());
            release.countDown();write.get(3,TimeUnit.SECONDS);read.get(3,TimeUnit.SECONDS);
            verify(client).ReadInt16("MW10000");assertEquals("SUCCESS",c.getCommunicationState());
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test public void failedScannerSendIsConnectionFailureWithoutResponseTimeout() {
        Connection c=new Connection();DeviceHealthProperties.ScannerHeartbeat cfg=new DeviceHealthProperties.ScannerHeartbeat();cfg.setEnabled(true);
        EmbeddedChannel channel=new EmbeddedChannel(new ChannelOutboundHandlerAdapter(){
            @Override public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise){ReferenceCountUtil.release(msg);promise.setFailure(new java.io.IOException("broken pipe"));}
        },new HeartbeatHandler(c,cfg,3));c.nowActive(channel.newSucceededFuture());
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
        assertEquals("OFFLINE",c.getStatusCode());assertEquals(0,c.getConsecutiveTimeouts());channel.finishAndReleaseAll();
    }
}
