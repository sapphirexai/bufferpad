package cn.tpl.opc.netty;

import cn.tpl.opc.service.impl.NettyServiceImpl;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeviceHealthSnapshotTest {
    @Test public void restSnapshotContainsIndependentTransportAndCommunicationFields() {
        Connection c=new Connection();c.setId(1L);c.setType(0);
        EmbeddedChannel channel=new EmbeddedChannel();c.nowActive(channel.newSucceededFuture());
        ConnectionMgr mgr=mock(ConnectionMgr.class);ConcurrentHashMap<Long,Connection> map=new ConcurrentHashMap<>();map.put(1L,c);when(mgr.getConnections()).thenReturn(map);
        NettyServiceImpl api=new NettyServiceImpl();ReflectionTestUtils.setField(api,"connectionMgr",mgr);
        DeviceInfoDTO pending=api.getDevicesStatus(0).get(0);
        assertEquals("CONNECTED",pending.getTransportState());assertEquals("UNVERIFIED",pending.getCommunicationState());assertEquals("PASSIVE",pending.getMonitoringMode());
        c.recordRequest("SCANNER_HEARTBEAT");c.recordRequestTimeout(1,null);
        DeviceInfoDTO failed=api.getDevicesStatus(0).get(0);
        assertEquals("DISCONNECTED",failed.getTransportState());assertEquals("TIMEOUT",failed.getCommunicationState());assertEquals(Integer.valueOf(1),failed.getConsecutiveTimeouts());
        assertNotNull(failed.getLastRequestAt());channel.finishAndReleaseAll();
    }
}
