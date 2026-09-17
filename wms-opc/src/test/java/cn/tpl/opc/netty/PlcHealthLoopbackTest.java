package cn.tpl.opc.netty;

import HslCommunication.Profinet.Inovance.InovanceTcpNet;
import org.junit.Test;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Real HSL/Modbus TCP round trips against an isolated, read-only simulated device. */
public class PlcHealthLoopbackTest {
    @Test public void realProtocolReadSucceedsAndExceptionIsNotNetworkOffline() throws Exception {
        try (Peer peer = new Peer()) {
            Connection c=connection(peer);DeviceHealthProperties.PlcProbe cfg=config();PlcHealthMonitor monitor=new PlcHealthMonitor();
            monitor.probe(c,cfg,3);assertEquals("SUCCESS",c.getCommunicationState());
            peer.mode="REJECT";monitor.probe(c,cfg,3);assertEquals("DEGRADED",c.getStatusCode());assertTrue(c.isActive());
            peer.mode="NORMAL";monitor.probe(c,cfg,3);assertEquals("SUCCESS",c.getCommunicationState());
            assertEquals(Arrays.asList(3,3,3),peer.functions);c.nowDead();
        }
    }
    @Test public void silentTcpPeerFailsOnlyAfterThreeSentReads() throws Exception {
        try (Peer peer = new Peer()) {
            peer.mode="SILENT";Connection c=connection(peer);DeviceHealthProperties.PlcProbe cfg=config();PlcHealthMonitor monitor=new PlcHealthMonitor();
            assertEquals("VERIFYING",c.getStatusCode());
            for(int i=1;i<=3;i++) { monitor.probe(c,cfg,3);assertEquals(i,c.getConsecutiveTimeouts());assertEquals(i<3?"RETRYING":"TIMEOUT",c.getStatusCode()); }
            assertTrue(c.isDead());assertEquals(Arrays.asList(3,3,3),peer.functions);
            peer.mode="NORMAL";c.nowActive(client(peer));assertEquals("TIMEOUT",c.getStatusCode());
            monitor.probe(c,cfg,3);assertEquals("SUCCESS",c.getCommunicationState());c.nowDead();
        }
    }
    private Connection connection(Peer peer) { Connection c=new Connection();c.setId(1L);c.setType(2);c.nowActive(client(peer));return c; }
    private InovanceTcpNet client(Peer peer) { InovanceTcpNet c=new InovanceTcpNet("127.0.0.1",peer.server.getLocalPort(),(byte)1);c.setConnectTimeOut(1000);c.setReceiveTimeOut(250);assertTrue(c.ConnectServer().IsSuccess);return c; }
    private DeviceHealthProperties.PlcProbe config() { DeviceHealthProperties.PlcProbe c=new DeviceHealthProperties.PlcProbe();c.setEnabled(true);c.setAddress("MW10000");return c; }
    private static class Peer implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,10,InetAddress.getByName("127.0.0.1"));
        final ExecutorService threads=Executors.newCachedThreadPool();final Set<Socket> clients=ConcurrentHashMap.newKeySet();
        final List<Integer> functions=new CopyOnWriteArrayList<>();volatile String mode="NORMAL";
        Peer() throws IOException { threads.submit(()->{while(!server.isClosed())try {Socket s=server.accept();clients.add(s);threads.submit(()->serve(s));}catch(IOException closed){break;}}); }
        void serve(Socket socket) {
            try (Socket s=socket) {
                DataInputStream in=new DataInputStream(s.getInputStream());OutputStream out=s.getOutputStream();
                while(!s.isClosed()) {
                    byte[] header=new byte[7];in.readFully(header);int length=((header[4]&255)<<8)|(header[5]&255);
                    byte[] body=new byte[length-1];in.readFully(body);int function=body[0]&255;functions.add(function);
                    if("SILENT".equals(mode))continue;
                    byte[] response="REJECT".equals(mode)?new byte[]{(byte)(function|128),2}:new byte[]{(byte)function,2,0,42};
                    header[4]=0;header[5]=(byte)(response.length+1);out.write(header);out.write(response);out.flush();
                }
            } catch(IOException closed) { } finally {clients.remove(socket);}
        }
        @Override public void close() throws Exception {server.close();for(Socket c:clients)c.close();threads.shutdownNow();threads.awaitTermination(3,TimeUnit.SECONDS);}
    }
}
