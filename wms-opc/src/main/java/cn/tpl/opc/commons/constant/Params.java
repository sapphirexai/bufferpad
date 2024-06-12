package cn.tpl.opc.commons.constant;

/**
 * Author: Luo Guowen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/7
 * 常量参数K-V管理
 */
public final class Params {
    private Params() {
    }

    /**
     * 设备类型
     * DEVICE_TYPE_KEY_SCANNER 扫码器
     * DEVICE_TYPE_KEY_PLC 三菱PLC
     * DEVICE_TYPE_KEY_HC_PLC 汇川PLC
     */
    public static final int
            DEVICE_TYPE_KEY_SCANNER = 0,
            DEVICE_TYPE_KEY_SL_PLC = 1,
            DEVICE_TYPE_KEY_HC_PLC = 2;

    /**
     * NETTY连接状态名
     * NETTY_CONNECTION_KEY_STATUS_DISCONNECTED 未连接
     * NETTY_CONNECTION_KEY_STATUS_ACTIVE 活跃中
     */
    public static final int
            NETTY_CONNECTION_KEY_STATUS_DISCONNECTED = 0,
            NETTY_CONNECTION_KEY_STATUS_ACTIVE = 1;

    /**
     * NETTY连接状态值
     * NETTY_CONNECTION_VAL_STATUS_DISCONNECTED 未连接
     * NETTY_CONNECTION_VAL_STATUS_ACTIVE 活跃中
     */
    public static final String
            NETTY_CONNECTION_VAL_STATUS_DISCONNECTED = "未连接",
            NETTY_CONNECTION_VAL_STATUS_ACTIVE = "活跃中";


    /**
     * 扫码器安装顺序
     */
    public static final int
            SCANNER_SEQ_KEY_1 = 1,
            SCANNER_SEQ_KEY_2 = 2;
    /**
     * 扫码器安装顺序对应位置
     */
    public static final String
            SCANNER_SEQ_VAL_1 = "上",
            SCANNER_SEQ_VAL_2 = "下";
}
