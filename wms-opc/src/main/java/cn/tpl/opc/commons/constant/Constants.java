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
     * NETTY连接超时时间，单位：毫秒
     */
    public static final int NETTY_CONNECT_TIMEOUT_MILLIS = 3500;

    /**
     * 产线，全部
     */
    public static final int WORK_LINE_ALL = 0;

    /**
     * HEARTBEAT_2_PLC_VAL 给PLC的心跳值
     * DEFAULT_2_PLC_VAL 默认写入值PLC的值
     */
    public static final int
            HEARTBEAT_2_PLC_VAL = 0,
            DEFAULT_2_PLC_VAL = 1;

    /**
     * PLC地址类型
     * PLC_ADDR_TYPE_SCAN_FAILED 扫码失败
     * PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM 扫码超过最大次数
     * PLC_ADDR_TYPE_SCAN_SUCCESS 扫码成功
     * PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM 重新扫码超过最大次数
     * PLC_ADDR_TYPE_RE_SCAN_SUCCESS 重新扫码成功
     * PLC_ADDR_TYPE_RE_SCAN_SUCCESS 心跳
     * PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_UP 上缓冲垫扫码成功开口数
     * PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_UP 上缓冲垫重新扫码成功开口数
     * PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_DOWN 下缓冲垫扫码成功开口数
     * PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_DOWN 下缓冲垫重新扫码成功开口数
     */
    public static final int
            PLC_ADDR_TYPE_SCAN_FAILED = 0,
            PLC_ADDR_TYPE_SCAN_OVER_MAXIMUM = 1,
            PLC_ADDR_TYPE_SCAN_SUCCESS = 2,
            PLC_ADDR_TYPE_RE_SCAN_OVER_MAXIMUM = 3,
            PLC_ADDR_TYPE_RE_SCAN_SUCCESS = 4,
            PLC_ADDR_TYPE_HEART_BEAT = 5,
            PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_UP = 6,
            PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_UP = 7,
            PLC_ADDR_TYPE_SCAN_SUCCESS_OPEN_COUNT_DOWN = 8,
            PLC_ADDR_TYPE_RE_SCAN_SUCCESS_OPEN_COUNT_DOWN = 9;

    /**
     * 缓冲垫默认最大使用次数
     * 宁波甬强默认600次
     * 汕头超声默认500次
     * CUSHION_DEFAULT_MAX_USE_CONT 通用最大次数
     */
    public static final int
            CUSHION_DEFAULT_MAX_USE_CONT = 500;

    /**
     * 新增缓冲垫时默认已使用次数
     */
    public static final int CUSHION_ADD_DEFAULT_USED_COUNT = 1;

    /**
     * 扫码器有效扫码间隔
     */
    public static final int SCANNER_EFFECTIVE_INTERVAL_MILLIS = 2 * 60 * 60 * 1000;

    /**
     * 扫码器数据
     * SCANNER_DATA_STX 帧头
     * SCANNER_DATA_ETX 帧尾
     */
    public static final char SCANNER_MSG_STX = 0x02, SCANNER_MSG_ETX = 0x03;

    public static final String SCANNER_XZ_STX = "[TPL_STX]", SCANNER_XZ_ETX = "[TPL_ETX]";
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
