package cn.tpl.opc.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtils {
    // 将 4 个字节的字节数组转换为整数，默认为大端字节序（高位字节在前）
    public static int bytesToInt(byte[] bytes) {
        return bytesToInt(bytes, 0,1 );
    }

    // 将指定长度的字节数组转换为整数，默认为大端字节序（高位字节在前）
    public static int bytesToInt(byte[] bytes, int startIndex, int length) {
        return bytesToInt(bytes, startIndex, length, true);
    }

    // 将 4 个字节的字节数组转换为整数，可指定字节序（是否为大端字节序）
    public static int bytesToInt(byte[] bytes, boolean isBigEndian) {
        return bytesToInt(bytes, 0, 1);
    }

    // 将指定长度的字节数组转换为整数，可指定字节序（是否为大端字节序）
    public static int bytesToInt(byte[] bytes, int startIndex, int length, boolean isBigEndian) {
        if (startIndex < 0 || length < 0 || startIndex + length > bytes.length) {
            throw new IllegalArgumentException("Invalid start index or length");
        }

        int value = 0;
        if (isBigEndian) {
            for (int i = 0; i < length; i++) {
                value = (value << 8) | (bytes[startIndex + i] & 0xFF);
            }
        } else {
            for (int i = length - 1; i >= 0; i--) {
                value = (value << 8) | (bytes[startIndex + i] & 0xFF);
            }
        }

        return value;
    }


    // 将 4 个字节的数组转换为单精度浮点数，默认使用大端字节序（高位字节在前）
    public static float bytesToFloat(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    // 将 8 个字节的数组转换为双精度浮点数，默认使用大端字节序（高位字节在前）
    public static double bytesToDouble(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getDouble();
    }
}