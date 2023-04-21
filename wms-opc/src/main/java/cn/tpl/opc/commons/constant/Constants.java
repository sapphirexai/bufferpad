package cn.tpl.opc.commons.constant;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/11
 * 普通常量管理
 */
public final class Constants {
    private Constants() {
    }

    /**
     * 缓冲垫二维码前缀
     */
    public static final String
            CUSHION_INFO_QR_CODE_PREFIX_P = "P",
            CUSHION_INFO_QR_CODE_PREFIX_T = "T";

    /**
     * 新增缓冲垫时默认使用次数
     */
    public static final int CUSHION_INGO_ADD_DEFAULT_USED_COUNT = 1;

    /**
     * 扫码器有效扫码间隔
     */
    public static final int SCANNER_EFFECTIVE_INTERVAL_MILLIS = 60 * 60 * 1000;

    /**
     * 扫码器数据
     * SCANNER_DATA_STX 帧头
     * SCANNER_DATA_ETX 帧尾
     */
    public static final char SCANNER_MSG_STX = 0x02, SCANNER_MSG_ETX = 0x03;

    /**
     * 扫码器消息
     * SCANNER_MSG_HEART_BEAT 心跳包
     * SCANNER_MSG_NO_READ 未读到数据
     */
    public static final String
            SCANNER_MSG_HEART_BEAT = "HeartBeat",
            SCANNER_MSG_NO_READ = "NoRead";

    /**
     * Netty连接重置间隔时间，单位：秒
     */
    public static final long NETTY_CONNECTION_RESET_INTERVAL_SEC = 1800L;

}
