package cn.tpl.opc.util;

import HslCommunication.Profinet.Melsec.MelsecMcNet;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/1/16
 * PLC操作集合
 */
@Slf4j
public class PLCUtils {
    private PLCUtils() {
    }

    /**
     * 操作PLC前先ping
     *
     * @param melsecMcNet PLC连接操作器
     * @return ping的结果, true: 失败; false: 成功
     */
    public static boolean pingPLCFailed(MelsecMcNet melsecMcNet) {
        try {
            if (null == melsecMcNet) return true;

            if (melsecMcNet.IpAddressPing()) return false;

        } catch (IOException e) {
            log.error("pingPLC, error => ", e);
            return true;
        }

        log.info("pingPLC, failed");
        return true;
    }
}
