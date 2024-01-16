package cn.tpl.opc.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/1/16
 * 网络操作集合
 */
@Slf4j
public class NetUtils {
    /**
     * ping操作超时时间，单位：毫秒
     */
    public static final int PING_TIMEOUT_MILLIS = 3000;

    private NetUtils() {
    }

    /**
     * ping操作
     *
     * @param host 连接地址
     * @return ping的结果, true: 失败; false: 成功
     */
    public static boolean pingFailed(String host) {
        log.info("ping, host => {}", host);
        try {
            if (StrUtil.isEmpty(host)) return true;

            if (InetAddress.getByName(host).isReachable(PING_TIMEOUT_MILLIS))
                return false;
        } catch (IOException e) {
            log.error("ping, error => ", e);
            return true;
        }

        log.info("ping, failed");
        return true;
    }
}
