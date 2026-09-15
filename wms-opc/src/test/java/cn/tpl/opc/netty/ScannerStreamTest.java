package cn.tpl.opc.netty;

import cn.tpl.opc.commons.dto.event.EventBusMsgCushionQrCode;
import cn.tpl.opc.infrastructure.scanner.ScannerMessageParser;
import cn.tpl.opc.netty.handler.MsgHandler;
import cn.tpl.opc.netty.handler.ScannerFrameDecoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class ScannerStreamTest {
    private void assertCodes(List<String> expected, byte[]... packets) {
        Connection connection = new Connection(); connection.setId(1L); connection.setWorkLine(1);
        List<String> received = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel(new ScannerFrameDecoder(),
                new MsgHandler(connection, e -> received.add(((EventBusMsgCushionQrCode)e).getQrCode()), new ScannerMessageParser()));
        connection.nowActive(channel.newSucceededFuture());
        try {
            for (byte[] packet : packets) channel.writeInbound(Unpooled.wrappedBuffer(packet));
            assertEquals(expected, received);
        } finally { channel.finishAndReleaseAll(); }
    }
    private byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    @Test public void standardFrameMaySplitAtEveryByteIncludingUtf8() {
        byte[] frame = bytes("\u0002缓冲垫-A\u0003");
        byte[][] packets = new byte[frame.length][];
        for(int i=0;i<frame.length;i++) packets[i]=new byte[]{frame[i]};
        assertCodes(Arrays.asList("缓冲垫-A"), packets);
    }
    @Test public void tplFrameMaySplitInsideEitherDelimiter() {
        assertCodes(Arrays.asList("TPL-A"), bytes("[TP"), bytes("L_STX]TPL-A[TPL_E"), bytes("TX]"));
    }
    @Test public void mixedCoalescedFramesRemainSeparateAndHeartbeatIsNotScan() {
        assertCodes(Arrays.asList("A", "B", null, "C"), bytes("\u0002A\u0003[TPL_STX]B[TPL_ETX]\u0002HeartBeat\u0003\u0002NoRead\u0003\u0002C\u0003"));
    }
    @Test public void partialFrameNeverTriggersPrematureScan() {
        assertCodes(Collections.emptyList(), bytes("\u0002NOT-FINISHED"));
    }
    @Test public void emptyAndInvalidFramesAreIgnoredAndNextFrameRecovers() {
        assertCodes(Arrays.asList("OK"), bytes("noiseHeartBeat\u0002\u0003\u0002OK\u0003"));
    }
    @Test public void newStartRecoversAfterTruncatedFrame() {
        assertCodes(Arrays.asList("OK"), bytes("\u0002truncated\u0002OK\u0003"));
    }
    @Test public void oversizeAndMalformedUtf8CannotPolluteNextScan() {
        assertCodes(Arrays.asList("OK"), bytes("\u0002" + "X".repeat(2000) + "\u0003"), new byte[]{2,(byte)0xff,3}, bytes("\u0002OK\u0003"));
    }
    @Test public void bareControlMessagesCanBeSplitAndDelimited() {
        assertCodes(Arrays.asList((String)null), bytes("Hea"), bytes("rtBeat\r\nNo"), bytes("Read\r\n"));
    }
    @Test public void keywordsWithinFramedBarcodeRemainData() {
        assertCodes(Arrays.asList("A-NoRead", "B-HeartBeat"), bytes("\u0002A-NoRead\u0003\u0002B-HeartBeat\u0003"));
    }
}
