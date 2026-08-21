package cn.tpl.opc.netty;

import HslCommunication.Core.Net.NetworkBase.NetworkDeviceBase;
import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Profinet.Siemens.SiemensS7Net;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.infrastructure.plc.PlcIoResult;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the real HSL Siemens client over TCP. The loopback peer implements
 * only the four S7 frames used here (COTP connect, setup communication, one
 * WriteVar and one ReadVar); it is intentionally not a mock of Connection.
 */
public class SiemensS7LoopbackIntegrationTest {
    @Test
    public void s71200ConnectsWritesAndReadsDbWordOverRealTcp() throws Exception {
        exercise(DeviceTypeEnum.SIEMENS_S7_1200_PLC, "DB10.DBW24", (short) 0x1234,
                0x84, 10, 24 * 8);
    }

    @Test
    public void s71500ConnectsWritesAndReadsMarkerWordOverRealTcp() throws Exception {
        exercise(DeviceTypeEnum.SIEMENS_S7_1500_PLC, "MW10", (short) -1234,
                0x83, 0, 10 * 8);
    }

    private void exercise(DeviceTypeEnum model, String address, short expectedValue,
                          int expectedArea, int expectedDb, int expectedBitOffset) throws Exception {
        try (S7LoopbackPeer peer = new S7LoopbackPeer()) {
            NetworkDeviceBase genericClient = Connector.createPlcClient(
                    model, InetAddress.getLoopbackAddress().getHostAddress(), peer.getPort());
            assertTrue(genericClient instanceof SiemensS7Net);
            SiemensS7Net client = (SiemensS7Net) genericClient;
            client.setConnectTimeOut((int) Duration.ofSeconds(2).toMillis());
            client.setReceiveTimeOut((int) Duration.ofSeconds(2).toMillis());

            OperateResult connected = client.ConnectServer();
            assertTrue("S7 connect failed: " + connected.Message, connected.IsSuccess);

            Connection connection = new Connection();
            connection.setId(20L);
            connection.setType(model.getCode());
            connection.setName(model.getLabel());
            connection.nowActive(client);

            PlcIoResult<Void> writeResult = connection.write(address, expectedValue);
            assertTrue("S7 write failed: " + writeResult.getMessage(), writeResult.isSuccess());

            PlcIoResult<Short> readResult = connection.readInt16(address);
            assertTrue("S7 read failed: " + readResult.getMessage(), readResult.isSuccess());
            assertEquals(Short.valueOf(expectedValue), readResult.getContent());

            connection.nowDead("test complete", null);
            assertTrue(connection.isDead());
            assertTrue(connection.isNoPLCNet());

            peer.awaitCompletion();
            assertRequest(peer.getWriteRequest(), 0x05, expectedArea, expectedDb, expectedBitOffset);
            assertRequest(peer.getReadRequest(), 0x04, expectedArea, expectedDb, expectedBitOffset);
            assertEquals(expectedValue, peer.getWrittenValue());
        }
    }

    private void assertRequest(byte[] request, int function, int area, int dbNumber, int bitOffset) {
        assertTrue("S7 request is unexpectedly short", request.length >= 31);
        assertEquals(function, request[17] & 0xFF);
        assertEquals(dbNumber, ((request[25] & 0xFF) << 8) | (request[26] & 0xFF));
        assertEquals(area, request[27] & 0xFF);
        assertEquals(bitOffset, ((request[28] & 0xFF) << 16)
                | ((request[29] & 0xFF) << 8) | (request[30] & 0xFF));
    }

    private static final class S7LoopbackPeer implements AutoCloseable {
        private static final byte[] COTP_CONNECT_CONFIRM = hex(
                "03 00 00 16 11 D0 00 01 00 01 00 C0 01 0A C1 02 01 00 C2 02 01 02");
        private static final byte[] SETUP_COMMUNICATION_ACK = hex(
                "03 00 00 1B 02 F0 80 32 03 00 00 00 01 00 08 00 00 00 00 F0 00 00 01 00 01 03 C0");

        private final ServerSocket serverSocket;
        private final ExecutorService executor;
        private final Future<?> serverTask;
        private volatile byte[] writeRequest;
        private volatile byte[] readRequest;
        private volatile short writtenValue;

        private S7LoopbackPeer() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "s7-loopback-peer");
                thread.setDaemon(true);
                return thread;
            });
            serverTask = executor.submit(() -> {
                serve();
                return null;
            });
        }

        private void serve() throws IOException {
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(2_000);
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();

                readTpkt(input); // COTP connection request
                send(output, COTP_CONNECT_CONFIRM);

                readTpkt(input); // S7 setup communication request
                send(output, SETUP_COMMUNICATION_ACK);

                writeRequest = readTpkt(input);
                if ((writeRequest[17] & 0xFF) != 0x05) {
                    throw new IOException("expected S7 WriteVar request");
                }
                writtenValue = (short) (((writeRequest[writeRequest.length - 2] & 0xFF) << 8)
                        | (writeRequest[writeRequest.length - 1] & 0xFF));
                send(output, writeAck(writeRequest));

                readRequest = readTpkt(input);
                if ((readRequest[17] & 0xFF) != 0x04) {
                    throw new IOException("expected S7 ReadVar request");
                }
                send(output, readAck(readRequest, writtenValue));
            }
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private byte[] getWriteRequest() {
            return writeRequest;
        }

        private byte[] getReadRequest() {
            return readRequest;
        }

        private short getWrittenValue() {
            return writtenValue;
        }

        private void awaitCompletion() throws Exception {
            try {
                serverTask.get(3, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                throw e;
            }
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            executor.shutdownNow();
            assertTrue("mock S7 peer did not stop", executor.awaitTermination(3, TimeUnit.SECONDS));
        }

        private static byte[] writeAck(byte[] request) {
            byte[] response = hex("03 00 00 16 02 F0 80 32 03 00 00 00 00 00 02 00 01 00 00 05 01 FF");
            copyPduReference(request, response);
            return response;
        }

        private static byte[] readAck(byte[] request, short value) {
            byte[] response = hex(
                    "03 00 00 1B 02 F0 80 32 03 00 00 00 00 00 02 00 06 00 00 04 01 FF 04 00 10 00 00");
            copyPduReference(request, response);
            response[25] = (byte) ((value >>> 8) & 0xFF);
            response[26] = (byte) (value & 0xFF);
            return response;
        }

        private static void copyPduReference(byte[] request, byte[] response) {
            response[11] = request[11];
            response[12] = request[12];
        }

        private static byte[] readTpkt(InputStream input) throws IOException {
            byte[] header = input.readNBytes(4);
            if (header.length == 0) throw new EOFException("socket closed before TPKT header");
            if (header.length != 4) throw new EOFException("incomplete TPKT header");
            if ((header[0] & 0xFF) != 0x03) throw new IOException("invalid TPKT version");
            int length = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            if (length < 4) throw new IOException("invalid TPKT length: " + length);
            byte[] body = input.readNBytes(length - 4);
            if (body.length != length - 4) throw new EOFException("incomplete TPKT body");
            byte[] packet = new byte[length];
            System.arraycopy(header, 0, packet, 0, 4);
            System.arraycopy(body, 0, packet, 4, body.length);
            return packet;
        }

        private static void send(OutputStream output, byte[] packet) throws IOException {
            output.write(packet);
            output.flush();
        }

        private static byte[] hex(String text) {
            String[] parts = text.trim().split("\\s+");
            byte[] result = new byte[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return result;
        }
    }
}
