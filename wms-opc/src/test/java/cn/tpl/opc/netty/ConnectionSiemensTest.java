package cn.tpl.opc.netty;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Siemens.SiemensS7Net;
import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConnectionSiemensTest {
    @Test
    public void routesInt16ReadAndWriteThroughSiemensClient() {
        SiemensS7Net client = mock(SiemensS7Net.class);
        when(client.Write("DB1.DBW0", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        when(client.ReadInt16("DB1.DBW2")).thenReturn(OperateResultExOne.CreateSuccessResult((short) 7));

        Connection connection = new Connection();
        connection.nowActive(client);

        PlcIoResult<Void> write = connection.write("DB1.DBW0", (short) 1);
        PlcIoResult<Short> read = connection.readInt16("DB1.DBW2");

        assertTrue(write.isSuccess());
        assertTrue(read.isSuccess());
        assertEquals(Short.valueOf((short) 7), read.getContent());
        verify(client).Write("DB1.DBW0", (short) 1);
        verify(client).ReadInt16("DB1.DBW2");
    }

    @Test
    public void closesSiemensTransportWhenConnectionGoesOffline() {
        SiemensS7Net client = mock(SiemensS7Net.class);
        Connection connection = new Connection();
        connection.nowActive(client);
        assertFalse(connection.isNoPLCNet());

        connection.nowDead("测试断开", 10000);

        verify(client).ConnectClose();
        assertTrue(connection.isNoPLCNet());
    }

    @Test
    public void stillRoutesMitsubishiWriteThroughUnifiedPlcClient() {
        MelsecMcNet client = mock(MelsecMcNet.class);
        when(client.Write("D6600", (short) 1)).thenReturn(OperateResult.CreateSuccessResult());
        Connection connection = new Connection();
        connection.nowActive(client);

        assertTrue(connection.write("D6600", (short) 1).isSuccess());
        verify(client).Write("D6600", (short) 1);
    }

    @Test
    public void stillRoutesInovanceReadThroughUnifiedPlcClient() {
        InovanceTcpNet client = mock(InovanceTcpNet.class);
        when(client.ReadInt16("MW10000")).thenReturn(OperateResultExOne.CreateSuccessResult((short) 5));
        Connection connection = new Connection();
        connection.nowActive(client);

        assertEquals(Short.valueOf((short) 5), connection.readInt16("MW10000").getContent());
        verify(client).ReadInt16("MW10000");
    }
}
