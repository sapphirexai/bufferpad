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
     * 产线，全部
     */
    public static final int WORK_LINE_ALL = 0;

    /**
     * PLC数据交互地址
     */
    public static final String
            PLC_DATA_ADDRESS_D9000 = "D9000",
            PLC_DATA_ADDRESS_D9001 = "D9001",
            PLC_DATA_ADDRESS_D9002 = "D9002";

    /**
     * 缓冲垫二维码前缀
     */
    public static final String
            CUSHION_QR_CODE_PREFIX_P = "P",
            CUSHION_QR_CODE_PREFIX_T = "T";

    /**
     * 缓冲垫默认最大使用次数
     * CUSHION_DEFAULT_MAX_USE_COUNT_P 二维码P开头
     * CUSHION_DEFAULT_MAX_USE_COUNT_T 二维码T开头
     */
    public static final int
            CUSHION_DEFAULT_MAX_USE_COUNT_P = 100,
            CUSHION_DEFAULT_MAX_USE_COUNT_T = 150;

    /**
     * 新增缓冲垫时默认已使用次数
     */
    public static final int CUSHION_ADD_DEFAULT_USED_COUNT = 1;

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
    public static final long NETTY_CONNECTION_RESET_INTERVAL_SEC = 100L;

    /**
     * Sse消息主题
     * SSE_MSG_TOPIC_DEVICE_STATUS 设备状态
     * SSE_MSG_TOPIC_CUSHION_INFO 缓冲垫数据
     */
    public static final String
            SSE_MSG_TOPIC_DEVICE_STATUS = "deviceStatus",
            SSE_MSG_TOPIC_CUSHION_INFO = "cushionInfo";

    /**
     * 接口请求结果消息
     * RESULT_MSG_CUSHION_ADD_FAILED 新增缓冲垫失败
     * RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED 增加缓冲垫使用次数失败
     * RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX 使用次数达到最大值
     * RESULT_MSG_CUSHION_INVALID_SCAN 缓冲垫无效扫码
     */
    public static final String
            RESULT_MSG_CUSHION_ADD_FAILED = "新增缓冲垫失败！",
            RESULT_MSG_CUSHION_ADD_USED_COUNT_FAILED = "增加缓冲垫使用次数失败！",
            RESULT_MSG_CUSHION_USED_COUNT_REACHED_MAX = "使用次数已达到最大次数！",
            RESULT_MSG_CUSHION_INVALID_SCAN = "扫码间隔时间不足1小时，无效扫码！";
}
