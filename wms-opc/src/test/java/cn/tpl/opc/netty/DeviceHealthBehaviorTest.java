package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import cn.tpl.opc.netty.handler.HeartbeatHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import javax.validation.Validation;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceHealthBehaviorTest {
    @Test public void passiveScannerNeverSendsApplicationHeartbeatOrTimesOut() {
        Connection c = new Connection();
        EmbeddedChannel channel = new EmbeddedChannel(new HeartbeatHandler(c)); c.nowActive(channel.newSucceededFuture());
        for (int i=0; i<100; i++) {
            channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
            channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
            channel.runScheduledPendingTasks();
        }
        assertNull(channel.readOutbound()); assertEquals("VERIFYING", c.getStatusCode());
        assertNull(c.getLastRequestAt()); assertEquals(0, c.getConsecutiveTimeouts()); channel.finishAndReleaseAll();
    }
    @Test public void heartbeatCountsOnlyRequestsThatWereSentAndRecoversOnResponse() throws Exception {
        Connection c = new Connection(); DeviceHealthProperties.ScannerHeartbeat cfg = new DeviceHealthProperties.ScannerHeartbeat();
        cfg.setEnabled(true); cfg.setResponseTimeoutSeconds(1);
        EmbeddedChannel channel = new EmbeddedChannel(new HeartbeatHandler(c, cfg, 3)); c.nowActive(channel.newSucceededFuture());
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
        ByteBuf sent = channel.readOutbound(); assertEquals("\u0002HeartBeat\u0003", sent.toString(CharsetUtil.UTF_8)); sent.release();
        assertEquals("SCANNER_HEARTBEAT", c.getLastRequestKind());
        assertEquals("VERIFYING", c.getStatusCode()); // send completion is not a protocol response
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT); assertNull(channel.readOutbound());
        Thread.sleep(1100); channel.runScheduledPendingTasks();
        assertEquals("RETRYING", c.getStatusCode()); assertEquals(1, c.getConsecutiveTimeouts());
        c.scannerResponse(channel); assertEquals("SUCCESS", c.getCommunicationState()); assertEquals(0, c.getConsecutiveTimeouts());
        channel.finishAndReleaseAll();
    }
    @Test public void oldHeartbeatDeadlineDoesNotAffectNewChannel() throws Exception {
        Connection c = new Connection(); DeviceHealthProperties.ScannerHeartbeat cfg = new DeviceHealthProperties.ScannerHeartbeat();
        cfg.setEnabled(true); cfg.setResponseTimeoutSeconds(1);
        EmbeddedChannel old = new EmbeddedChannel(new HeartbeatHandler(c, cfg, 3)); c.nowActive(old.newSucceededFuture());
        old.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
        ByteBuf sent=old.readOutbound();sent.release();c.nowDead();
        EmbeddedChannel replacement=new EmbeddedChannel(); c.nowActive(replacement.newSucceededFuture());
        Thread.sleep(1100); old.runScheduledPendingTasks();
        assertEquals(0,c.getConsecutiveTimeouts());assertEquals("VERIFYING",c.getStatusCode());
        old.finishAndReleaseAll();replacement.finishAndReleaseAll();
    }
    @Test public void probeIsExplicitAndReadOnly() {
        Connection c = new Connection(); InovanceTcpNet client=mock(InovanceTcpNet.class);c.nowActive(client);
        DeviceHealthProperties.PlcProbe cfg = probe();cfg.setEnabled(false);
        new PlcHealthMonitor().probe(c,cfg,3); verify(client,never()).ReadInt16(anyString());assertNull(c.getLastRequestAt());
        cfg.setEnabled(true);when(client.ReadInt16("MW10000")).thenReturn(OperateResultExOne.CreateSuccessResult((short) 42));
        new PlcHealthMonitor().probe(c,cfg,3);
        verify(client).ReadInt16("MW10000");verify(client,never()).Write(anyString(),anyShort());
        assertEquals("SUCCESS",c.getCommunicationState());assertEquals("PLC_READ_PROBE",c.getLastRequestKind());
    }
    @Test public void threeTimeoutsBecomeFaultAndHandshakeAloneCannotClearIt() {
        Connection c = new Connection();InovanceTcpNet client=mock(InovanceTcpNet.class);c.nowActive(client);
        PlcIoResult<Void> timeout=PlcIoResult.failure(10000,"Read timed out");
        PlcHealthMonitor.applyFailure(c,timeout,3);assertEquals("RETRYING",c.getStatusCode());assertTrue(c.isActive());
        PlcHealthMonitor.applyFailure(c,timeout,3);assertEquals("RETRYING",c.getStatusCode());
        PlcHealthMonitor.applyFailure(c,timeout,3);assertEquals("TIMEOUT",c.getStatusCode());assertTrue(c.isDead());
        c.nowActive(mock(InovanceTcpNet.class));assertEquals("TIMEOUT",c.getStatusCode());assertTrue(c.isActive());
        c.markMonitoringSuccess();assertEquals("ONLINE",c.getStatusCode());assertEquals(0,c.getConsecutiveTimeouts());
    }
    @Test public void protocolRejectionIsNotOfflineAndProbeCanRecover() {
        Connection c=new Connection();c.nowActive(mock(InovanceTcpNet.class));
        PlcHealthMonitor.applyFailure(c,PlcIoResult.failure(2,"Illegal data address"),3);
        assertEquals("DEGRADED",c.getStatusCode());assertEquals("CONNECTED",c.getTransportState());
        c.markMonitoringSuccess();assertEquals("ONLINE",c.getStatusCode());
        c.markDegraded(85,"PLC禁止运行中写入");c.markMonitoringSuccess();
        assertEquals("DEGRADED",c.getStatusCode());assertNotNull(c.getLastCommunicationAt());
        c.markOnline("业务写入成功");assertEquals("SUCCESS",c.getCommunicationState());
    }
    @Test public void oldProbeResponseCannotValidateReplacementConnection() {
        Connection c=new Connection();InovanceTcpNet old=mock(InovanceTcpNet.class), replacement=mock(InovanceTcpNet.class);c.nowActive(old);
        when(old.ReadInt16(anyString())).thenAnswer(call->{ c.nowDead();c.nowActive(replacement);return OperateResultExOne.CreateSuccessResult((short) 1); });
        new PlcHealthMonitor().probe(c,probe(),3);assertEquals("VERIFYING",c.getStatusCode());assertNull(c.getLastCommunicationAt());
    }
    @Test public void invalidProbeAndHeartbeatConfigurationsAreRejected() {
        try (javax.validation.ValidatorFactory factory=Validation.buildDefaultValidatorFactory()) {
            DeviceHealthProperties props=new DeviceHealthProperties();DeviceHealthProperties.PlcProbe probe=new DeviceHealthProperties.PlcProbe();probe.setEnabled(true);props.getPlcProbes().put(1L,probe);
            assertFalse(factory.getValidator().validate(props).isEmpty());
            props.getPlcProbes().clear();DeviceHealthProperties.ScannerHeartbeat hb=new DeviceHealthProperties.ScannerHeartbeat();hb.setEnabled(true);hb.setResponseTimeoutSeconds(30);props.getScannerHeartbeats().put(2L,hb);
            assertFalse(factory.getValidator().validate(props).isEmpty());
        }
    }
    private DeviceHealthProperties.PlcProbe probe() { DeviceHealthProperties.PlcProbe cfg=new DeviceHealthProperties.PlcProbe();cfg.setEnabled(true);cfg.setAddress("MW10000");return cfg; }
}
