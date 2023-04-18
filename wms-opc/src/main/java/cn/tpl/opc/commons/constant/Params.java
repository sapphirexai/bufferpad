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
     * PLC请求参数键
     * PLC_REQUEST_KEY_CHECK_STATUS 连线状态确认
     */
    public static final String
            PLC_REQUEST_KEY_CHECK_STATUS = "D9001";

    /**
     * PLC请求参数值
     * PLC_REQUEST_VAL_CHECK_STATUS 连线状态确认
     */
    public static final int
            PLC_REQUEST_VAL_CHECK_STATUS = 1;


    /**
     * NETTY连接状态名
     * NETTY_CONNECTION_KEY_STATUS_DISCONNECTED 未连接
     * NETTY_CONNECTION_KEY_STATUS_CONNECTED 活跃中
     * NETTY_CONNECTION_KEY_STATUS_NO_RESPONSE 无响应
     */
    public static final int
            NETTY_CONNECTION_KEY_STATUS_DISCONNECTED = 0,
            NETTY_CONNECTION_KEY_STATUS_ACTIVE = 1,
            NETTY_CONNECTION_KEY_STATUS_NO_RESPONSE = 2;

    /**
     * NETTY连接状态值
     * NETTY_CONNECTION_VAL_STATUS_DISCONNECTED 未连接
     * NETTY_CONNECTION_VAL_STATUS_CONNECTED 活跃中
     * NETTY_CONNECTION_VAL_STATUS_NO_RESPONSE 无响应
     */
    public static final String
            NETTY_CONNECTION_VAL_STATUS_DISCONNECTED = "未连接",
            NETTY_CONNECTION_VAL_STATUS_ACTIVE = "活跃中",
            NETTY_CONNECTION_VAL_STATUS_NO_RESPONSE = "无响应";

}
