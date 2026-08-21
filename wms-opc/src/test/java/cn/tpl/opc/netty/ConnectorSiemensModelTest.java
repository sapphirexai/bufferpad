package cn.tpl.opc.netty;

import HslCommunication.Profinet.Siemens.SiemensPLCS;
import HslCommunication.Profinet.Siemens.SiemensS7Net;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ConnectorSiemensModelTest {
    @Test
    public void mapsDeviceTypesToTheMatchingS7HandshakeModel() {
        assertSame(SiemensPLCS.S1200,
                Connector.resolveSiemensPlcModel(DeviceTypeEnum.SIEMENS_S7_1200_PLC));
        assertSame(SiemensPLCS.S1500,
                Connector.resolveSiemensPlcModel(DeviceTypeEnum.SIEMENS_S7_1500_PLC));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonSiemensDeviceType() {
        Connector.resolveSiemensPlcModel(DeviceTypeEnum.MITSUBISHI_PLC);
    }

    @Test
    public void buildsS7TransportWithIsoTcpEndpointAndExpectedRackSlot() {
        SiemensS7Net client = (SiemensS7Net) Connector.createPlcClient(
                DeviceTypeEnum.SIEMENS_S7_1500_PLC, "192.0.2.15", 102);

        assertEquals("192.0.2.15", client.getIpAddress());
        assertEquals(102, client.getPort());
        assertEquals(0, client.getRack());
        assertEquals(0, client.getSlot());
    }

    @Test
    public void keepsExistingPlcTransportFactories() {
        assertTrue(Connector.createPlcClient(DeviceTypeEnum.MITSUBISHI_PLC, "127.0.0.1", 6000)
                instanceof HslCommunication.Profinet.Melsec.MelsecMcNet);
        assertTrue(Connector.createPlcClient(DeviceTypeEnum.INOVANCE_PLC, "127.0.0.1", 502)
                instanceof HslCommunication.Profinet.Inovance.InovanceTcpNet);
    }
}
