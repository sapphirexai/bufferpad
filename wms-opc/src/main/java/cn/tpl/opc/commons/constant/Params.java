package cn.tpl.opc.commons.constant;

import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;

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
     * DEVICE_TYPE_KEY_SL_PLC 三菱PLC
     * DEVICE_TYPE_KEY_HC_PLC 汇川PLC
     * DEVICE_TYPE_KEY_SIEMENS_S7_PLC 西门子S7 PLC
     */
    public static final int
            DEVICE_TYPE_KEY_SCANNER = DeviceTypeEnum.SCANNER.getCode(),
            DEVICE_TYPE_KEY_SL_PLC = DeviceTypeEnum.MITSUBISHI_PLC.getCode(),
            DEVICE_TYPE_KEY_HC_PLC = DeviceTypeEnum.INOVANCE_PLC.getCode(),
            DEVICE_TYPE_KEY_SIEMENS_S7_PLC = DeviceTypeEnum.SIEMENS_S7_PLC.getCode();

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
     * 扫码器安装顺序历史默认值。当前安装位置已改为 device_install_position 表维护，
     * device_info.install_seq 保存安装位置表 ID。
     */
    @Deprecated
    public static final int
            SCANNER_SEQ_KEY_1 = 1,
            SCANNER_SEQ_KEY_2 = 2,
            SCANNER_SEQ_KEY_3 = 3,
            SCANNER_SEQ_KEY_4 = 4;
    /**
     * 扫码器安装顺序历史默认位置。
     */
    @Deprecated
    public static final String
            SCANNER_SEQ_VAL_1 = "上",
            SCANNER_SEQ_VAL_2 = "下",
            SCANNER_SEQ_VAL_3 = "间层1",
            SCANNER_SEQ_VAL_4 = "间层2";
}
