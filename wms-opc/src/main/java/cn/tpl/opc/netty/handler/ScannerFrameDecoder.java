package cn.tpl.opc.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Keeps TCP packet boundaries out of the scanner protocol, including split UTF-8 characters. */
public class ScannerFrameDecoder extends ByteToMessageDecoder {
    private static final byte[] TPL_START = "[TPL_STX]".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TPL_END = "[TPL_ETX]".getBytes(StandardCharsets.US_ASCII);
    private static final byte[][] TOKENS = {
            "HeartBeat".getBytes(StandardCharsets.US_ASCII), "NoRead".getBytes(StandardCharsets.US_ASCII)};
    private static final int MAX_FRAME_BYTES = 1024;

    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.isReadable()) {
            int start = in.readerIndex(), end = in.writerIndex();
            int first = in.getUnsignedByte(start);
            if (first == '\r' || first == '\n' || first == ' ') { in.skipBytes(1); continue; }
            if (first == 2 || prefix(in, start, TPL_START)) {
                boolean standard = first == 2;
                int prefixLength = standard ? 1 : TPL_START.length;
                if (end - start < prefixLength) return;
                int terminator = -1, nested = -1;
                for (int i = start + prefixLength; i < end; i++) {
                    if (standard ? in.getByte(i) == 3 : matches(in, i, TPL_END)) { terminator = i; break; }
                    if (in.getByte(i) == 2 || matches(in, i, TPL_START)) { nested = i; break; }
                }
                if (nested >= 0) { in.readerIndex(nested); continue; }
                if (terminator < 0) {
                    if (in.readableBytes() > MAX_FRAME_BYTES) in.skipBytes(in.readableBytes());
                    return;
                }
                int length = terminator - start + (standard ? 1 : TPL_END.length);
                if (length <= MAX_FRAME_BYTES) out.add(in.readRetainedSlice(length));
                else in.skipBytes(length);
                continue;
            }
            boolean tokenConsumed = false;
            for (byte[] token : TOKENS) {
                if (!prefix(in, start, token)) continue;
                if (in.readableBytes() < token.length) return;
                if (end == start + token.length || boundary(in.getUnsignedByte(start + token.length))) {
                    out.add(in.readRetainedSlice(token.length)); tokenConsumed = true; break;
                }
            }
            if (tokenConsumed) continue;
            // Discard noise up to a new framed record; keywords inside noise are not control messages.
            int next = start + 1;
            while (next < end && in.getByte(next) != 2 && in.getByte(next) != '['
                    && in.getByte(next) != '\r' && in.getByte(next) != '\n') next++;
            in.readerIndex(next);
        }
    }
    private boolean boundary(int value) { return value == 2 || value == '[' || value == '\r' || value == '\n' || value == ' '; }
    private boolean prefix(ByteBuf in, int start, byte[] bytes) {
        int count = Math.min(in.writerIndex() - start, bytes.length);
        for (int i = 0; i < count; i++) if (in.getByte(start + i) != bytes[i]) return false;
        return count > 0;
    }
    private boolean matches(ByteBuf in, int start, byte[] bytes) {
        return in.writerIndex() - start >= bytes.length && prefix(in, start, bytes);
    }
}
