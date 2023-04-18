package cn.tpl.opc.util;

import HslCommunication.Core.Types.OperateResultExOne;

public class ByteResultUtils {
    // 将 OperateResultExOne<byte[]> 对象中的字节数组转换为整数，默认为大端字节序（高位字节在前）
    public static int bytesToInt(OperateResultExOne<byte[]> result) {
        return ByteUtils.bytesToInt(result.Content);
    }

    // 将 OperateResultExOne<byte[]> 对象中指定长度的字节数组转换为整数，默认为大端字节序（高位字节在前）
    public static int bytesToInt(OperateResultExOne<byte[]> result, int startIndex, int length) {
        return ByteUtils.bytesToInt(result.Content, startIndex, length);
    }

    // 将 OperateResultExOne<byte[]> 对象中的字节数组转换为整数，可指定字节序（是否为大端字节序）
    public static int bytesToInt(OperateResultExOne<byte[]> result, boolean isBigEndian) {
        return ByteUtils.bytesToInt(result.Content, isBigEndian);
    }

    // 将 OperateResultExOne<byte[]> 对象中指定长度的字节数组转换为整数，可指定字节序（是否为大端字节序）
    public static int bytesToInt(OperateResultExOne<byte[]> result, int startIndex, int length, boolean isBigEndian) {
        return ByteUtils.bytesToInt(result.Content, startIndex, length, isBigEndian);
    }


}